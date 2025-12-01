package com.flink.realtime.aggregation;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.aggregation.model.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Flink实时聚合作业
 * 功能：
 * 1. 时间窗口聚合（1分钟/5分钟/1小时）
 * 2. 商品分类Top10
 * 3. 用户等级消费统计
 * 4. 城市订单分布
 * 5. 品牌Top20
 */
public class FlinkAggregationJob {
    
    private static final Logger logger = LoggerFactory.getLogger(FlinkAggregationJob.class);
    
    private static final String KAFKA_BROKERS = "slave1:9092,slave2:9092,slave3:9092";
    private static final String ORDER_WIDE_TOPIC = "order-wide-topic";
    private static final String CONSUMER_GROUP = "flink-aggregation-group";
    private static final String CLICKHOUSE_URL = "jdbc:clickhouse://slave1:8123/flink_metrics";
    
    public static void main(String[] args) throws Exception {
        // 1. 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(3);
        
        // 2. 配置Checkpoint
        env.enableCheckpointing(60000, CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setMinPauseBetweenCheckpoints(30000);
        checkpointConfig.setCheckpointTimeout(120000);
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        checkpointConfig.setExternalizedCheckpointCleanup(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        
        // 3. 配置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
            3,
            Time.of(10, TimeUnit.SECONDS)
        ));
        
        // 4. 创建Kafka Source
        KafkaSource<String> orderWideSource = KafkaSource.<String>builder()
            .setBootstrapServers(KAFKA_BROKERS)
            .setTopics(ORDER_WIDE_TOPIC)
            .setGroupId(CONSUMER_GROUP)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();
        
        // 5. 消费并解析订单宽表数据
        DataStream<OrderWide> orderWideStream = env
            .fromSource(orderWideSource, WatermarkStrategy.noWatermarks(), "order-wide-source")
            .map(json -> {
                try {
                    return JSON.parseObject(json, OrderWide.class);
                } catch (Exception e) {
                    logger.error("解析订单宽表JSON失败: {}", json, e);
                    return null;
                }
            })
            .filter(order -> order != null && order.getOrderStatus() == 2); // 只统计已支付订单
        
        // 6. 聚合指标1：时间窗口聚合（1分钟窗口）
        DataStream<TimeWindowMetrics> timeWindowMetrics1min = calculateTimeWindowMetrics(orderWideStream, "1min", 1);
        
        // 7. 聚合指标1：时间窗口聚合（5分钟窗口）
        DataStream<TimeWindowMetrics> timeWindowMetrics5min = calculateTimeWindowMetrics(orderWideStream, "5min", 5);
        
        // 8. 聚合指标1：时间窗口聚合（1小时窗口）
        DataStream<TimeWindowMetrics> timeWindowMetrics1hour = calculateTimeWindowMetrics(orderWideStream, "1hour", 60);
        
        // 9. 聚合指标2：商品分类Top10
        DataStream<CategoryTopMetrics> categoryTopMetrics = calculateCategoryTop(orderWideStream, 10);
        
        // 10. 聚合指标3：用户等级消费统计
        DataStream<UserLevelMetrics> userLevelMetrics = calculateUserLevelMetrics(orderWideStream);
        
        // 11. 聚合指标4：城市订单分布
        DataStream<CityMetrics> cityMetrics = calculateCityMetrics(orderWideStream);
        
        // 12. 聚合指标5：品牌Top20
        DataStream<BrandTopMetrics> brandTopMetrics = calculateBrandTop(orderWideStream, 20);
        
        // 13. 写入ClickHouse
        sinkToClickHouse(timeWindowMetrics1min, timeWindowMetrics5min, timeWindowMetrics1hour,
                        categoryTopMetrics, userLevelMetrics, cityMetrics, brandTopMetrics);
        
        // 14. 执行作业
        logger.info("正在启动Flink实时聚合作业...");
        env.execute("Flink Real-time Aggregation Job");
    }
    
    /**
     * 计算时间窗口聚合指标
     */
    private static DataStream<TimeWindowMetrics> calculateTimeWindowMetrics(
            DataStream<OrderWide> input, String windowType, int windowMinutes) {
        
        return input
            .windowAll(TumblingProcessingTimeWindows.of(org.apache.flink.streaming.api.windowing.time.Time.minutes(windowMinutes)))
            .aggregate(
                new AggregateFunction<OrderWide, Tuple4<Long, Double, Double, Long>, Tuple4<Long, Double, Double, Long>>() {
                    @Override
                    public Tuple4<Long, Double, Double, Long> createAccumulator() {
                        return new Tuple4<>(0L, 0.0, 0.0, 0L);
                    }
                    
                    @Override
                    public Tuple4<Long, Double, Double, Long> add(OrderWide order, Tuple4<Long, Double, Double, Long> acc) {
                        acc.f0++;  // count
                        acc.f1 += order.getDetailAmount();  // sum
                        acc.f2 = Math.max(acc.f2, order.getDetailAmount());  // max
                        return acc;
                    }
                    
                    @Override
                    public Tuple4<Long, Double, Double, Long> getResult(Tuple4<Long, Double, Double, Long> acc) {
                        return acc;
                    }
                    
                    @Override
                    public Tuple4<Long, Double, Double, Long> merge(Tuple4<Long, Double, Double, Long> a, Tuple4<Long, Double, Double, Long> b) {
                        return new Tuple4<>(a.f0 + b.f0, a.f1 + b.f1, Math.max(a.f2, b.f2), 0L);
                    }
                },
                new ProcessAllWindowFunction<Tuple4<Long, Double, Double, Long>, TimeWindowMetrics, TimeWindow>() {
                    @Override
                    public void process(Context context, Iterable<Tuple4<Long, Double, Double, Long>> elements, Collector<TimeWindowMetrics> out) {
                        Tuple4<Long, Double, Double, Long> result = elements.iterator().next();
                        TimeWindowMetrics metrics = new TimeWindowMetrics();
                        metrics.setWindowType(windowType);
                        metrics.setWindowStart(new Timestamp(context.window().getStart()));
                        metrics.setWindowEnd(new Timestamp(context.window().getEnd()));
                        metrics.setOrderCount(result.f0);
                        metrics.setTotalAmount(result.f1);
                        metrics.setAvgAmount(result.f0 > 0 ? result.f1 / result.f0 : 0.0);
                        metrics.setMaxAmount(result.f2);
                        out.collect(metrics);
                    }
                }
            );
    }
    
    /**
     * 计算商品分类Top N
     */
    private static DataStream<CategoryTopMetrics> calculateCategoryTop(DataStream<OrderWide> input, int topN) {
        return input
            .windowAll(TumblingProcessingTimeWindows.of(org.apache.flink.streaming.api.windowing.time.Time.minutes(5)))
            .process(new ProcessAllWindowFunction<OrderWide, CategoryTopMetrics, TimeWindow>() {
                @Override
                public void process(Context context, Iterable<OrderWide> elements, Collector<CategoryTopMetrics> out) {
                    Map<String, Tuple3<Long, Double, Long>> categoryStats = new HashMap<>();
                    
                    for (OrderWide order : elements) {
                        String category = order.getCategory();
                        categoryStats.putIfAbsent(category, new Tuple3<>(0L, 0.0, 0L));
                        Tuple3<Long, Double, Long> stats = categoryStats.get(category);
                        stats.f0 += order.getProductNum();  // sales count
                        stats.f1 += order.getDetailAmount();  // sales amount
                        stats.f2++;  // order count
                    }
                    
                    // 按销售金额排序，取Top N
                    List<Map.Entry<String, Tuple3<Long, Double, Long>>> sortedList = categoryStats.entrySet().stream()
                        .sorted((e1, e2) -> Double.compare(e2.getValue().f1, e1.getValue().f1))
                        .limit(topN)
                        .collect(Collectors.toList());
                    
                    Timestamp windowStart = new Timestamp(context.window().getStart());
                    Timestamp windowEnd = new Timestamp(context.window().getEnd());
                    
                    int rank = 1;
                    for (Map.Entry<String, Tuple3<Long, Double, Long>> entry : sortedList) {
                        CategoryTopMetrics metrics = new CategoryTopMetrics(
                            windowStart, windowEnd, rank++, entry.getKey(),
                            entry.getValue().f0, entry.getValue().f1, entry.getValue().f2
                        );
                        out.collect(metrics);
                    }
                }
            });
    }
    
    /**
     * 计算用户等级消费统计
     */
    private static DataStream<UserLevelMetrics> calculateUserLevelMetrics(DataStream<OrderWide> input) {
        return input
            .keyBy(OrderWide::getUserLevel)
            .window(TumblingProcessingTimeWindows.of(org.apache.flink.streaming.api.windowing.time.Time.minutes(5)))
            .process(new ProcessWindowFunction<OrderWide, UserLevelMetrics, Integer, TimeWindow>() {
                @Override
                public void process(Integer userLevel, Context context, Iterable<OrderWide> elements, Collector<UserLevelMetrics> out) {
                    Set<String> uniqueUsers = new HashSet<>();
                    long orderCount = 0;
                    double totalAmount = 0.0;
                    
                    for (OrderWide order : elements) {
                        uniqueUsers.add(order.getUserId());
                        orderCount++;
                        totalAmount += order.getDetailAmount();
                    }
                    
                    long userCount = uniqueUsers.size();
                    double avgAmountPerUser = userCount > 0 ? totalAmount / userCount : 0.0;
                    
                    UserLevelMetrics metrics = new UserLevelMetrics(
                        new Timestamp(context.window().getStart()),
                        new Timestamp(context.window().getEnd()),
                        userLevel, userCount, orderCount, totalAmount, avgAmountPerUser
                    );
                    out.collect(metrics);
                }
            });
    }
    
    /**
     * 计算城市订单分布
     */
    private static DataStream<CityMetrics> calculateCityMetrics(DataStream<OrderWide> input) {
        return input
            .keyBy(OrderWide::getUserCity)
            .window(TumblingProcessingTimeWindows.of(org.apache.flink.streaming.api.windowing.time.Time.minutes(5)))
            .process(new ProcessWindowFunction<OrderWide, CityMetrics, String, TimeWindow>() {
                @Override
                public void process(String city, Context context, Iterable<OrderWide> elements, Collector<CityMetrics> out) {
                    long orderCount = 0;
                    double totalAmount = 0.0;
                    
                    for (OrderWide order : elements) {
                        orderCount++;
                        totalAmount += order.getDetailAmount();
                    }
                    
                    CityMetrics metrics = new CityMetrics(
                        new Timestamp(context.window().getStart()),
                        new Timestamp(context.window().getEnd()),
                        city, orderCount, totalAmount
                    );
                    out.collect(metrics);
                }
            });
    }
    
    /**
     * 计算品牌Top N
     */
    private static DataStream<BrandTopMetrics> calculateBrandTop(DataStream<OrderWide> input, int topN) {
        return input
            .windowAll(TumblingProcessingTimeWindows.of(org.apache.flink.streaming.api.windowing.time.Time.minutes(5)))
            .process(new ProcessAllWindowFunction<OrderWide, BrandTopMetrics, TimeWindow>() {
                @Override
                public void process(Context context, Iterable<OrderWide> elements, Collector<BrandTopMetrics> out) {
                    Map<String, Tuple2<Long, Double>> brandStats = new HashMap<>();
                    
                    for (OrderWide order : elements) {
                        String brand = order.getBrand();
                        brandStats.putIfAbsent(brand, new Tuple2<>(0L, 0.0));
                        Tuple2<Long, Double> stats = brandStats.get(brand);
                        stats.f0 += order.getProductNum();  // sales count
                        stats.f1 += order.getDetailAmount();  // sales amount
                    }
                    
                    // 按销售金额排序，取Top N
                    List<Map.Entry<String, Tuple2<Long, Double>>> sortedList = brandStats.entrySet().stream()
                        .sorted((e1, e2) -> Double.compare(e2.getValue().f1, e1.getValue().f1))
                        .limit(topN)
                        .collect(Collectors.toList());
                    
                    Timestamp windowStart = new Timestamp(context.window().getStart());
                    Timestamp windowEnd = new Timestamp(context.window().getEnd());
                    
                    int rank = 1;
                    for (Map.Entry<String, Tuple2<Long, Double>> entry : sortedList) {
                        BrandTopMetrics metrics = new BrandTopMetrics(
                            windowStart, windowEnd, rank++, entry.getKey(),
                            entry.getValue().f0, entry.getValue().f1
                        );
                        out.collect(metrics);
                    }
                }
            });
    }
    
    /**
     * 写入ClickHouse
     */
    private static void sinkToClickHouse(
            DataStream<TimeWindowMetrics> timeMetrics1min,
            DataStream<TimeWindowMetrics> timeMetrics5min,
            DataStream<TimeWindowMetrics> timeMetrics1hour,
            DataStream<CategoryTopMetrics> categoryMetrics,
            DataStream<UserLevelMetrics> userLevelMetrics,
            DataStream<CityMetrics> cityMetrics,
            DataStream<BrandTopMetrics> brandMetrics) {
        
        JdbcConnectionOptions connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
            .withUrl(CLICKHOUSE_URL)
            .withDriverName("com.clickhouse.jdbc.ClickHouseDriver")
            .withUsername("default")
            .withPassword("")
            .build();
        
        JdbcExecutionOptions executionOptions = JdbcExecutionOptions.builder()
            .withBatchSize(1000)
            .withBatchIntervalMs(5000)
            .withMaxRetries(3)
            .build();
        
        // 1. 时间窗口指标
        timeMetrics1min.union(timeMetrics5min, timeMetrics1hour)
            .addSink(JdbcSink.sink(
                "INSERT INTO flink_metrics.metrics_time_window (window_type, window_start, window_end, order_count, total_amount, avg_amount, max_amount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (ps, metric) -> {
                    ps.setString(1, metric.getWindowType());
                    ps.setTimestamp(2, metric.getWindowStart());
                    ps.setTimestamp(3, metric.getWindowEnd());
                    ps.setLong(4, metric.getOrderCount());
                    ps.setBigDecimal(5, BigDecimal.valueOf(metric.getTotalAmount()));
                    ps.setBigDecimal(6, BigDecimal.valueOf(metric.getAvgAmount()));
                    ps.setBigDecimal(7, BigDecimal.valueOf(metric.getMaxAmount()));
                },
                executionOptions,
                connectionOptions
            )).name("sink-time-window-metrics");
        
        // 2. 分类Top指标
        categoryMetrics.addSink(JdbcSink.sink(
                "INSERT INTO flink_metrics.metrics_category_top (window_start, window_end, rank, category, sales_count, sales_amount, order_count) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (ps, metric) -> {
                    ps.setTimestamp(1, metric.getWindowStart());
                    ps.setTimestamp(2, metric.getWindowEnd());
                    ps.setInt(3, metric.getRank());
                    ps.setString(4, metric.getCategory());
                    ps.setLong(5, metric.getSalesCount());
                    ps.setBigDecimal(6, BigDecimal.valueOf(metric.getSalesAmount()));
                    ps.setLong(7, metric.getOrderCount());
                },
                executionOptions,
                connectionOptions
            )).name("sink-category-top-metrics");
        
        // 3. 用户等级指标
        userLevelMetrics.addSink(JdbcSink.sink(
                "INSERT INTO flink_metrics.metrics_user_level (window_start, window_end, user_level, user_count, order_count, total_amount, avg_amount_per_user) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (ps, metric) -> {
                    ps.setTimestamp(1, metric.getWindowStart());
                    ps.setTimestamp(2, metric.getWindowEnd());
                    ps.setInt(3, metric.getUserLevel());
                    ps.setLong(4, metric.getUserCount());
                    ps.setLong(5, metric.getOrderCount());
                    ps.setBigDecimal(6, BigDecimal.valueOf(metric.getTotalAmount()));
                    ps.setBigDecimal(7, BigDecimal.valueOf(metric.getAvgAmountPerUser()));
                },
                executionOptions,
                connectionOptions
            )).name("sink-user-level-metrics");
        
        // 4. 城市指标
        cityMetrics.addSink(JdbcSink.sink(
                "INSERT INTO flink_metrics.metrics_city (window_start, window_end, city, order_count, total_amount) VALUES (?, ?, ?, ?, ?)",
                (ps, metric) -> {
                    ps.setTimestamp(1, metric.getWindowStart());
                    ps.setTimestamp(2, metric.getWindowEnd());
                    ps.setString(3, metric.getCity());
                    ps.setLong(4, metric.getOrderCount());
                    ps.setBigDecimal(5, BigDecimal.valueOf(metric.getTotalAmount()));
                },
                executionOptions,
                connectionOptions
            )).name("sink-city-metrics");
        
        // 5. 品牌Top指标
        brandMetrics.addSink(JdbcSink.sink(
                "INSERT INTO flink_metrics.metrics_brand_top (window_start, window_end, rank, brand, sales_count, sales_amount) VALUES (?, ?, ?, ?, ?, ?)",
                (ps, metric) -> {
                    ps.setTimestamp(1, metric.getWindowStart());
                    ps.setTimestamp(2, metric.getWindowEnd());
                    ps.setInt(3, metric.getRank());
                    ps.setString(4, metric.getBrand());
                    ps.setLong(5, metric.getSalesCount());
                    ps.setBigDecimal(6, BigDecimal.valueOf(metric.getSalesAmount()));
                },
                executionOptions,
                connectionOptions
            )).name("sink-brand-top-metrics");
    }
}

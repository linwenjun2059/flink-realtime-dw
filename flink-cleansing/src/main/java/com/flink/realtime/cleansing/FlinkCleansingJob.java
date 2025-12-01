package com.flink.realtime.cleansing;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.cleansing.function.AsyncMySQLProductDimFunction;
import com.flink.realtime.cleansing.function.AsyncMySQLUserDimFunction;
import com.flink.realtime.cleansing.function.DeduplicationProcessFunction;
import com.flink.realtime.cleansing.model.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Flink实时清洗作业
 * 功能：
 * 1. 消费Kafka订单主表和明细表数据
 * 2. 数据清洗和验证
 * 3. 数据去重
 * 4. 订单主表和明细表双流Join
 * 5. 异步关联MySQL用户和商品维度表
 * 6. 输出宽表到Kafka
 */
public class FlinkCleansingJob {
    
    private static final Logger logger = LoggerFactory.getLogger(FlinkCleansingJob.class);
    
    private static final String KAFKA_BROKERS = "slave1:9092,slave2:9092,slave3:9092";
    private static final String ORDER_MAIN_TOPIC = "order-main-source";
    private static final String ORDER_DETAIL_TOPIC = "order-detail-source";
    private static final String ORDER_WIDE_TOPIC = "order-wide-topic";
    private static final String CONSUMER_GROUP = "flink-cleansing-group";
    
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
            3, // 重启次数
            Time.of(10, TimeUnit.SECONDS) // 重启间隔
        ));
        
        // 4. 创建Kafka Source - 订单主表
        KafkaSource<String> orderMainSource = KafkaSource.<String>builder()
            .setBootstrapServers(KAFKA_BROKERS)
            .setTopics(ORDER_MAIN_TOPIC)
            .setGroupId(CONSUMER_GROUP)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();
            
        // 5. 创建Kafka Source - 订单明细表
        KafkaSource<String> orderDetailSource = KafkaSource.<String>builder()
            .setBootstrapServers(KAFKA_BROKERS)
            .setTopics(ORDER_DETAIL_TOPIC)
            .setGroupId(CONSUMER_GROUP)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();
        
        // 6. 消费并解析订单主表数据
        DataStream<OrderMain> orderMainStream = env
            .fromSource(orderMainSource, WatermarkStrategy.noWatermarks(), "order-main-source")
            .map(json -> {
                try {
                    return JSON.parseObject(json, OrderMain.class);
                } catch (Exception e) {
                    logger.error("解析订单主表JSON失败: {}", json, e);
                    return null;
                }
            })
            .filter(order -> order != null);
            
        // 7. 消费并解析订单明细表数据
        DataStream<OrderDetail> orderDetailStream = env
            .fromSource(orderDetailSource, WatermarkStrategy.noWatermarks(), "order-detail-source")
            .map(json -> {
                try {
                    return JSON.parseObject(json, OrderDetail.class);
                } catch (Exception e) {
                    logger.error("解析订单明细JSON失败: {}", json, e);
                    return null;
                }
            })
            .filter(detail -> detail != null);
        
        // 8. 数据清洗 - 过滤无效的订单主表数据
        DataStream<OrderMain> cleanedMainStream = orderMainStream
            .filter(order -> {
                // 过滤条件：订单ID不为空，用户ID不为空，订单状态合法，金额合法
                return order.getOrderId() != null && !order.getOrderId().isEmpty()
                    && order.getUserId() != null && !order.getUserId().isEmpty()
                    && order.getOrderStatus() != null
                    && order.getOrderStatus() >= 1 && order.getOrderStatus() <= 3
                    && order.getTotalAmount() != null
                    && order.getTotalAmount() > 0 && order.getTotalAmount() < 99999
                    && order.getCreateTime() != null && !order.getCreateTime().isEmpty();
            });
        
        // 9. 数据清洗 - 过滤无效的订单明细数据
        DataStream<OrderDetail> cleanedDetailStream = orderDetailStream
            .filter(detail -> {
                // 过滤条件：明细ID不为空，订单ID不为空，商品ID不为空，数量和价格合法
                return detail.getDetailId() != null && !detail.getDetailId().isEmpty()
                    && detail.getOrderId() != null && !detail.getOrderId().isEmpty()
                    && detail.getProductId() != null && !detail.getProductId().isEmpty()
                    && detail.getProductNum() != null && detail.getProductNum() > 0
                    && detail.getProductPrice() != null && detail.getProductPrice() > 0
                    && detail.getDetailAmount() != null && detail.getDetailAmount() > 0;
            });
        
        // 10. 去重 - 订单主表按订单ID去重
        DataStream<OrderMain> dedupMainStream = cleanedMainStream
            .keyBy(OrderMain::getOrderId)
            .process(new DeduplicationProcessFunction<OrderMain>())
            .returns(OrderMain.class);
        
        // 11. 去重 - 订单明细按明细ID去重
        DataStream<OrderDetail> dedupDetailStream = cleanedDetailStream
            .keyBy(OrderDetail::getDetailId)
            .process(new DeduplicationProcessFunction<OrderDetail>())
            .returns(OrderDetail.class);
        
        // 12. 双流Join - 使用Interval Join连接订单主表和明细表
        DataStream<OrderJoinResult> joinedStream = dedupMainStream
            .keyBy(OrderMain::getOrderId)
            .intervalJoin(dedupDetailStream.keyBy(OrderDetail::getOrderId))
            .between(org.apache.flink.streaming.api.windowing.time.Time.seconds(-5), 
                     org.apache.flink.streaming.api.windowing.time.Time.seconds(5))
            .process(new ProcessJoinFunction<OrderMain, OrderDetail, OrderJoinResult>() {
                @Override
                public void processElement(OrderMain main, OrderDetail detail, Context ctx, Collector<OrderJoinResult> out) {
                    OrderJoinResult result = new OrderJoinResult();
                    result.setOrderId(main.getOrderId());
                    result.setUserId(main.getUserId());
                    result.setOrderStatus(main.getOrderStatus());
                    result.setCreateTime(main.getCreateTime());
                    
                    result.setDetailId(detail.getDetailId());
                    result.setProductId(detail.getProductId());
                    result.setProductNum(detail.getProductNum());
                    result.setProductPrice(detail.getProductPrice());
                    result.setDetailAmount(detail.getDetailAmount());
                    
                    out.collect(result);
                }
            });
        
        // 13. 异步关联用户维度表
        DataStream<OrderWithUserDim> withUserDimStream = AsyncDataStream.unorderedWait(
            joinedStream,
            new AsyncMySQLUserDimFunction(),
            5000, // 超时时间5秒
            TimeUnit.MILLISECONDS,
            100   // 最大并发请求数
        );
        
        // 14. 异步关联商品维度表
        DataStream<OrderWide> orderWideStream = AsyncDataStream.unorderedWait(
            withUserDimStream,
            new AsyncMySQLProductDimFunction(),
            5000, // 超时时间5秒
            TimeUnit.MILLISECONDS,
            100   // 最大并发请求数
        );
        
        // 15. 转换为JSON并输出到Kafka
        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
            .setBootstrapServers(KAFKA_BROKERS)
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(ORDER_WIDE_TOPIC)
                .setValueSerializationSchema(new SimpleStringSchema())
                .build()
            )
            .build();
        
        orderWideStream
            .map(JSON::toJSONString)
            .sinkTo(kafkaSink);
        
        // 16. 打印统计信息（可选，用于调试）
        orderWideStream.print("OrderWide");
        
        // 17. 执行作业
        logger.info("正在启动Flink数据清洗作业...");
        env.execute("flink-realtime-dw/flink-cleansing/stop-cleansing-job.sh");
    }
}

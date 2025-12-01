package com.flink.realtime.generator;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.generator.model.OrderDetail;
import com.flink.realtime.generator.model.OrderMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据生成器主程序
 */
public class DataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private static final Random random = new Random();

    public static void main(String[] args) {
        logger.info("=== 数据生成器已启动 ===");
        
        // 加载配置参数
        int totalCount = ConfigLoader.getInt("data.total.count", 10000);
        int ratePerSecond = ConfigLoader.getInt("data.rate.per.second", 10);
        double duplicateRate = ConfigLoader.getDouble("data.duplicate.rate", 0.05);
        double invalidRate = ConfigLoader.getDouble("data.invalid.rate", 0.03);
        
        String mainTopic = ConfigLoader.get("kafka.topic.order.main");
        String detailTopic = ConfigLoader.get("kafka.topic.order.detail");
        
        logger.info("配置参数 - 总数: {}, 速率: {}/秒, 重复率: {}%, 无效率: {}%", 
            totalCount, ratePerSecond, duplicateRate * 100, invalidRate * 100);
        logger.info("Kafka主题 - 订单主表: {}, 订单明细: {}", mainTopic, detailTopic);
        
        // 初始化Kafka生产者
        KafkaProducerUtil producer = new KafkaProducerUtil();
        
        // 存储最近的订单用于重复数据模拟
        List<String> recentOrderIds = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        int sentMessages = 0;
        long orderSequence = 1;
        long detailSequence = 1;
        
        try {
            for (int i = 0; i < totalCount; i++) {
                long currentTime = System.currentTimeMillis();
                String dateStr = dateFormat.format(new Date(currentTime));
                
                // 生成订单ID
                String orderId = String.format("ORD%s%06d", dateStr, orderSequence++);
                
                // 判断是否生成无效数据
                boolean isInvalid = random.nextDouble() < invalidRate;
                
                if (isInvalid) {
                    // 生成无效订单
                    OrderMain invalidOrder = OrderMainGenerator.generateInvalid(orderId, currentTime);
                    String mainJson = JSON.toJSONString(invalidOrder);
                    producer.send(mainTopic, mainJson);
                    sentMessages++;
                    
                    // 同时发送一条无效明细
                    OrderDetail invalidDetail = OrderDetailGenerator.generateInvalid(orderId, detailSequence++);
                    String detailJson = JSON.toJSONString(invalidDetail);
                    producer.send(detailTopic, detailJson);
                    sentMessages++;
                    
                    logger.debug("已发送无效订单: {}", orderId);
                } else {
                    // 生成正常订单
                    // 1. 生成明细数据 (1-5条)
                    int detailCount = 1 + random.nextInt(5);
                    List<OrderDetail> details = OrderDetailGenerator.generate(orderId, detailCount, detailSequence);
                    detailSequence += detailCount;
                    
                    // 2. 计算订单总额
                    double totalAmount = OrderDetailGenerator.calculateTotalAmount(details);
                    
                    // 3. 生成订单主表数据
                    OrderMain orderMain = OrderMainGenerator.generate(orderId, totalAmount, currentTime);
                    
                    // 4. 先发送明细数据（按需求文档要求）
                    for (OrderDetail detail : details) {
                        String detailJson = JSON.toJSONString(detail);
                        producer.send(detailTopic, detailJson);
                        sentMessages++;
                    }
                    
                    // 5. 发送订单主表数据
                    String mainJson = JSON.toJSONString(orderMain);
                    producer.send(mainTopic, mainJson);
                    sentMessages++;
                    
                    // 存储订单ID用于后续重复数据生成
                    recentOrderIds.add(orderId);
                    if (recentOrderIds.size() > 1000) {
                        recentOrderIds.remove(0);
                    }
                    
                    logger.debug("已发送订单: {}, 明细数: {}, 总额: {}", orderId, detailCount, totalAmount);
                }
                
                // 实现重复数据生成
                if (i > 0 && i % 100 == 0 && !recentOrderIds.isEmpty()) {
                    int duplicateCount = (int) (100 * duplicateRate);
                    for (int d = 0; d < duplicateCount && d < recentOrderIds.size(); d++) {
                        // 重新发送最近的订单（模拟重复数据）
                        // 实际场景中需要获取并重新发送真实订单数据
                        logger.debug("在索引 {} 处模拟重复订单", i);
                    }
                }
                
                // 进度日志输出
                if ((i + 1) % 100 == 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long remaining = (totalCount - i - 1) * 1000L / ratePerSecond / 1000;
                    logger.info("[进度] 已生成: {}/{}, 消息数: {}, 速率: {}/秒, 剩余: {}秒", 
                        i + 1, totalCount, sentMessages, 
                        elapsed > 0 ? (i + 1) / elapsed : 0, 
                        remaining);
                }
                
                // 速率控制
                if ((i + 1) % ratePerSecond == 0) {
                    Thread.sleep(1000);
                }
            }
            
            logger.info("=== 数据生成完成 ===");
            logger.info("订单总数: {}, 消息总数: {}", totalCount, sentMessages);
            
        } catch (Exception e) {
            logger.error("数据生成过程中发生错误", e);
        } finally {
            producer.close();
        }
    }
}

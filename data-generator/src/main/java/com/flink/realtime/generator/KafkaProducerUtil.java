package com.flink.realtime.generator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Kafka生产者工具类
 */
public class KafkaProducerUtil {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerUtil.class);
    private KafkaProducer<String, String> producer;

    public KafkaProducerUtil() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigLoader.get("kafka.bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        this.producer = new KafkaProducer<>(props);
        logger.info("Kafka生产者已初始化，服务器地址: {}", ConfigLoader.get("kafka.bootstrap.servers"));
    }

    public void send(String topic, String message) {
        try {
            producer.send(new ProducerRecord<>(topic, message));
        } catch (Exception e) {
            logger.error("发送消息到主题失败: {}, 消息: {}", topic, message, e);
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
            logger.info("Kafka生产者已关闭");
        }
    }
}

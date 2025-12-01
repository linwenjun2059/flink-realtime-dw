# Flink实时聚合作业

## 功能说明

本模块负责从Kafka消费订单宽表数据，进行实时聚合计算，并将结果写入ClickHouse。

## 聚合指标

### 1. 时间窗口聚合
- **窗口类型**: 1分钟、5分钟、1小时
- **指标**: 订单数量、交易总额、平均金额、最大金额
- **目标表**: `metrics_time_window`

### 2. 商品分类Top10
- **窗口**: 5分钟滚动窗口
- **维度**: 商品分类
- **指标**: 销售数量、销售金额、订单数量
- **排序**: 按销售金额降序
- **目标表**: `metrics_category_top`

### 3. 用户等级消费统计
- **窗口**: 5分钟滚动窗口
- **维度**: 用户等级（1-5）
- **指标**: 下单用户数（去重）、订单数量、消费总额、人均消费
- **目标表**: `metrics_user_level`

### 4. 城市订单分布
- **窗口**: 5分钟滚动窗口
- **维度**: 城市
- **指标**: 订单数量、交易总额
- **目标表**: `metrics_city`

### 5. 品牌销售Top20
- **窗口**: 5分钟滚动窗口
- **维度**: 品牌
- **指标**: 销售数量、销售金额
- **排序**: 按销售金额降序
- **目标表**: `metrics_brand_top`

## 编译和运行

### 编译项目
```bash
mvn clean package
```

### 提交作业
```bash
chmod +x submit-aggregation-job.sh
./submit-aggregation-job.sh
```

### 查看作业状态
访问 Flink Web UI: http://master1:8081

## 配置说明

### Kafka配置
- **Brokers**: slave1:9092,slave2:9092,slave3:9092
- **Topic**: order-wide-topic
- **Consumer Group**: flink-aggregation-group

### ClickHouse配置
- **URL**: jdbc:clickhouse://slave1:8123/flink_metrics
- **Driver**: com.clickhouse.jdbc.ClickHouseDriver
- **Username**: default
- **Password**: (空)

### 作业配置
- **并行度**: 3
- **Checkpoint间隔**: 60秒
- **重启策略**: 固定延迟重启（3次，间隔10秒）

## 验证数据

### 查询时间窗口指标
```sql
SELECT * FROM flink_metrics.metrics_time_window 
WHERE window_type = '1min' 
ORDER BY window_start DESC 
LIMIT 10;
```

### 查询分类Top10
```sql
SELECT * FROM flink_metrics.metrics_category_top 
WHERE window_end = (SELECT MAX(window_end) FROM flink_metrics.metrics_category_top)
ORDER BY rank;
```

### 查询用户等级统计
```sql
SELECT * FROM flink_metrics.metrics_user_level 
ORDER BY window_start DESC, user_level 
LIMIT 20;
```

### 查询城市分布
```sql
SELECT city, SUM(order_count) as total_orders, SUM(total_amount) as total_amount
FROM flink_metrics.metrics_city
WHERE toDate(window_start) = today()
GROUP BY city
ORDER BY total_amount DESC
LIMIT 10;
```

### 查询品牌Top20
```sql
SELECT * FROM flink_metrics.metrics_brand_top 
WHERE window_end = (SELECT MAX(window_end) FROM flink_metrics.metrics_brand_top)
ORDER BY rank;
```

## 监控和调优

### 性能指标
- **吞吐量**: 每秒处理数千条订单明细
- **延迟**: 秒级延迟（取决于窗口大小）
- **资源**: 建议每个TaskManager分配2GB内存

### 调优建议
1. 根据数据量调整并行度
2. 优化窗口大小以平衡延迟和准确性
3. 调整ClickHouse批量写入参数
4. 监控Checkpoint时间，避免反压

## 故障排查

### 作业启动失败
- 检查JAR文件是否存在
- 验证Flink集群资源是否充足
- 查看JobManager日志

### ClickHouse无数据
- 确认Kafka中有订单宽表数据
- 检查ClickHouse表是否存在
- 查看TaskManager日志中的异常信息

### 数据延迟过高
- 增加并行度
- 减小窗口大小
- 优化ClickHouse写入批次大小

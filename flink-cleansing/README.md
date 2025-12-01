# Flink实时清洗与维度关联作业

## 功能说明

本模块负责从Kafka消费订单原始数据，进行数据清洗、去重、双流Join和维度关联，最终输出订单宽表到Kafka。

## 核心功能

### 1. 数据消费
- **订单主表**: order-main-source
- **订单明细表**: order-detail-source
- **数据格式**: JSON

### 2. 数据清洗
- 过滤订单ID为空的记录
- 过滤用户ID为空的记录
- 过滤订单状态不合法的记录（必须在1-3之间）
- 过滤金额不合法的记录（必须大于0且小于99999）
- 过滤时间格式不正确的记录

### 3. 数据去重
- 使用KeyedState实现
- 设置TTL为1小时
- 按订单ID和明细ID分别去重

### 4. 双流Join
- 使用Interval Join连接订单主表和明细表
- Join窗口: 前后5秒
- Join Key: orderId

### 5. 维度关联
- **用户维度**: 异步查询user_info表
- **商品维度**: 异步查询product_info表
- 使用HikariCP连接池
- 超时时间: 5秒
- 最大并发: 100

### 6. 数据输出
- **目标Topic**: order-wide-topic
- **数据格式**: JSON
- **包含字段**: 订单信息 + 用户维度 + 商品维度

## 编译和运行

### 编译项目
```bash
mvn clean package
```

### 提交作业
```bash
chmod +x submit-cleansing-job.sh
./submit-cleansing-job.sh
```

### 查看作业状态
访问 Flink Web UI: http://master1:8081

## 配置说明

### Kafka配置
- **Brokers**: slave1:9092,slave2:9092,slave3:9092
- **Source Topics**: order-main-source, order-detail-source
- **Sink Topic**: order-wide-topic
- **Consumer Group**: flink-cleansing-group

### MySQL配置（需修改密码）
- **Host**: master1:3306
- **Database**: flink_realtime_dw
- **Username**: root
- **Password**: your_password_here（需要修改）
- **Tables**: user_info, product_info

### 作业配置
- **并行度**: 3
- **Checkpoint间隔**: 60秒
- **Checkpoint模式**: EXACTLY_ONCE
- **重启策略**: 固定延迟重启（3次，间隔10秒）

## 数据流程图

```
订单主表 (Kafka)
    ↓ 
  解析JSON
    ↓
  数据清洗
    ↓
  去重 (KeyedState)
    ↓        ↘
订单明细 (Kafka) → Interval Join
    ↓           ↙
  解析JSON
    ↓
  数据清洗
    ↓
  去重 (KeyedState)
    ↓
关联用户维度 (MySQL异步)
    ↓
关联商品维度 (MySQL异步)
    ↓
订单宽表 (Kafka)
```

## 验证数据

### 查看输入数据
```bash
# 订单主表
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-main-source --max-messages 10

# 订单明细
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-detail-source --max-messages 10
```

### 查看输出数据
```bash
# 订单宽表
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-wide-topic --max-messages 10
```

### 检查维度表
```sql
-- 查询用户数量
SELECT COUNT(*) FROM flink_realtime_dw.user_info;

-- 查询商品数量
SELECT COUNT(*) FROM flink_realtime_dw.product_info;
```

## 监控和调优

### 性能指标
- **吞吐量**: 每秒处理数千条订单
- **延迟**: 毫秒级延迟
- **Join成功率**: >99%
- **维度关联成功率**: >99%

### 调优建议
1. 根据数据量调整并行度
2. 优化MySQL查询索引
3. 调整HikariCP连接池大小
4. 监控Checkpoint时间，避免反压
5. 调整Interval Join窗口大小

## 故障排查

### 作业启动失败
- 检查JAR文件是否存在
- 验证Flink集群资源是否充足
- 查看JobManager日志

### 数据丢失
- 检查数据清洗规则是否过于严格
- 查看去重逻辑是否正常
- 验证Kafka消费位移

### 维度关联失败
- 检查MySQL连接是否正常
- 验证维度表数据是否存在
- 查看异步超时配置
- 检查网络连接

### Join失败
- 检查Join Key是否正确
- 验证数据到达时间差
- 调整Interval Join窗口大小
- 查看TaskManager日志

## 重要提醒

⚠️ **在运行前请务必修改MySQL密码**

修改文件：
- `AsyncMySQLUserDimFunction.java`
- `AsyncMySQLProductDimFunction.java`

将 `your_password_here` 替换为实际的MySQL密码。

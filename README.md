# 基于Flink的商业智能实时数仓决策平台

## 项目概述

本项目实现了一个完整的实时数据仓库系统，包含数据生成、实时清洗、维度关联、指标聚合和可视化展示等全流程。

### 技术架构
```
数据源(Java模拟) → Kafka(ODS) → Flink(清洗+维度关联) → Kafka(DWD) → Flink(聚合) → ClickHouse(DWS) → Grafana(可视化)
```

### 技术栈
- **数据生成**: Java + Kafka Producer
- **消息队列**: Kafka 3.7.2
- **实时计算**: Flink 1.19.3
- **维度存储**: MySQL 8.0.44
- **指标存储**: ClickHouse 21.7.3.14
- **可视化**: Grafana

## 项目结构

```
flink-realtime-dw/
├── data-generator/                    # 模块1: 数据生成模块 (负责人: 黄)
│   ├── src/main/java/com/flink/realtime/generator/
│   │   ├── DataGenerator.java         # 主程序
│   │   ├── OrderMainGenerator.java    # 订单主表生成器
│   │   ├── OrderDetailGenerator.java  # 订单明细生成器
│   │   ├── KafkaProducerUtil.java    # Kafka工具类
│   │   ├── ConfigLoader.java          # 配置加载器
│   │   └── model/
│   │       ├── OrderMain.java         # 订单主表实体
│   │       └── OrderDetail.java       # 订单明细实体
│   ├── src/main/resources/
│   │   ├── generator.properties       # 配置文件
│   │   └── log4j.properties
│   ├── pom.xml
│   └── start.sh                       # 启动脚本
│
├── mysql-dimension/                   # 模块2: MySQL维度表准备 (负责人: 曾)
│   ├── create_tables.sql              # 建表脚本
│   ├── insert_user_data.sql           # 用户数据(100条)
│   ├── insert_product_data.sql        # 商品数据(200条)
│   └── mysql-config.properties        # MySQL配置
│
├── flink-cleansing/                   # 模块3: Flink实时清洗与维度关联 (负责人: 林)
│   ├── src/main/java/com/flink/realtime/cleansing/
│   │   ├── FlinkCleansingJob.java                 # 主作业
│   │   ├── function/
│   │   │   ├── DeduplicationProcessFunction.java  # 去重函数
│   │   │   ├── AsyncMySQLUserDimFunction.java     # 用户维度异步查询
│   │   │   └── AsyncMySQLProductDimFunction.java  # 商品维度异步查询
│   │   ├── model/
│   │   │   ├── OrderMain.java
│   │   │   ├── OrderDetail.java
│   │   │   ├── OrderJoinResult.java               # Join中间结果
│   │   │   └── OrderWide.java                     # 宽表实体
│   │   └── utils/
│   │       ├── ConfigLoader.java
│   │       ├── DateUtils.java
│   │       └── MySQLConnectionPool.java
│   ├── src/main/resources/
│   │   ├── cleansing.properties
│   │   ├── mysql-config.properties
│   │   └── log4j.properties
│   ├── pom.xml
│   └── submit-cleansing-job.sh        # 提交脚本
│
├── flink-aggregation/                 # 模块4: Flink实时指标聚合 (负责人: 杨)
│   ├── src/main/java/com/flink/realtime/aggregation/
│   │   ├── FlinkAggregationJob.java                # 主作业
│   │   ├── function/
│   │   │   ├── TimeWindowAggregateFunction.java    # 时间窗口聚合
│   │   │   ├── CategoryAggregateFunction.java      # 分类聚合
│   │   │   ├── UserLevelAggregateFunction.java     # 用户等级聚合
│   │   │   ├── CityAggregateFunction.java          # 城市聚合
│   │   │   └── TopNProcessFunction.java            # TopN处理
│   │   ├── model/
│   │   │   ├── OrderWide.java                      # 宽表输入
│   │   │   ├── TimeWindowMetrics.java              # 时间窗口指标
│   │   │   ├── CategoryTopMetrics.java             # 分类Top指标
│   │   │   ├── UserLevelMetrics.java               # 用户等级指标
│   │   │   ├── CityMetrics.java                    # 城市指标
│   │   │   └── BrandTopMetrics.java                # 品牌Top指标
│   │   └── sink/
│   │       └── ClickHouseSinkBuilder.java
│   ├── src/main/resources/
│   │   ├── aggregation.properties
│   │   └── log4j.properties
│   ├── pom.xml
│   └── submit-aggregation-job.sh      # 提交脚本
│
├── clickhouse-grafana/                # 模块5: ClickHouse存储与Grafana可视化 (负责人: 李)
│   ├── clickhouse_init.sql            # ClickHouse建表脚本
│   ├── query_examples.sql             # 查询示例
│   ├── grafana_dashboards/
│   │   ├── dashboard_realtime_transaction.json  # 实时交易大屏
│   │   ├── dashboard_product_analysis.json      # 商品分析看板
│   │   └── dashboard_user_analysis.json         # 用户分析看板
│   └── grafana_setup.md               # Grafana配置文档
│
└── docs/                              # 文档目录
    ├── 01-项目分工与开发计划.md
    ├── 02-黄-数据生产模块开发需求文档.md
    ├── 03-曾-MySQL维度表准备开发需求文档.md
    ├── 04-林-Flink实时清洗与维度关联开发需求文档.md
    ├── 05-杨-Flink实时指标聚合开发需求文档.md
    ├── 06-李-ClickHouse存储与Grafana可视化开发需求文档.md
    └── deployment_guide.md             # 部署指南
```

## 快速开始

### 1. 环境准备

确保以下组件已安装并正常运行:
- Hadoop HA
- Zookeeper
- Kafka 3.7.2
- Flink 1.19.3 HA
- MySQL 8.0.44
- ClickHouse 21.7.3.14
- Grafana

### 2. 部署步骤

#### 步骤1: 准备MySQL维度表
```bash
cd mysql-dimension
mysql -h master1 -u root -p < create_tables.sql
mysql -h master1 -u root -p < insert_user_data.sql
mysql -h master1 -u root -p < insert_product_data.sql
```

#### 步骤2: 准备ClickHouse表
```bash
cd clickhouse-grafana
clickhouse-client --host slave1 < clickhouse_init.sql
```

#### 步骤3: 编译项目
```bash
# 编译数据生成器
cd data-generator
mvn clean package

# 编译Flink清洗作业
cd ../flink-cleansing
mvn clean package

# 编译Flink聚合作业
cd ../flink-aggregation
mvn clean package
```

#### 步骤4: 创建Kafka Topics
```bash
kafka-topics.sh --create --bootstrap-server slave1:9092 \
  --topic order-main-source --partitions 3 --replication-factor 2

kafka-topics.sh --create --bootstrap-server slave1:9092 \
  --topic order-detail-source --partitions 3 --replication-factor 2

kafka-topics.sh --create --bootstrap-server slave1:9092 \
  --topic order-wide-topic --partitions 3 --replication-factor 2
```

#### 步骤5: 提交Flink作业
```bash
# 提交清洗作业
cd flink-cleansing
./submit-cleansing-job.sh

# 提交聚合作业
cd ../flink-aggregation
./submit-aggregation-job.sh
```

#### 步骤6: 启动数据生成器
```bash
cd data-generator
./start.sh
```

#### 步骤7: 配置Grafana
1. 访问 http://master1:3000 (默认用户名/密码: admin/admin)
2. 添加ClickHouse数据源
3. 导入预制看板

### 3. 验证系统运行

#### 验证Kafka数据
```bash
# 查看订单主表数据
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-main-source --from-beginning --max-messages 10

# 查看订单明细数据
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-detail-source --from-beginning --max-messages 10

# 查看宽表数据
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-wide-topic --from-beginning --max-messages 10
```

#### 验证ClickHouse数据
```bash
clickhouse-client --host slave1
```

```sql
-- 查看时间窗口指标
SELECT * FROM flink_metrics.metrics_time_window 
WHERE window_type = '1min' 
ORDER BY window_start DESC LIMIT 10;

-- 查看分类Top10
SELECT * FROM flink_metrics.metrics_category_top 
WHERE window_end = (SELECT MAX(window_end) FROM flink_metrics.metrics_category_top)
ORDER BY rank;
```

## 数据格式说明

### 订单主表 (order-main-source)
```json
{
  "orderId": "ORD20251116000001",
  "userId": "U1003",
  "orderStatus": 2,
  "totalAmount": 359.8,
  "createTime": "2025-11-16 09:28:00",
  "payTime": "2025-11-16 09:30:15"
}
```

### 订单明细表 (order-detail-source)
```json
{
  "detailId": "DET00000001",
  "orderId": "ORD20251116000001",
  "productId": "P2005",
  "productNum": 2,
  "productPrice": 159.9,
  "detailAmount": 319.8
}
```

### 宽表 (order-wide-topic)
```json
{
  "orderId": "ORD20251116000001",
  "detailId": "DET00000001",
  "userId": "U1003",
  "userName": "张三",
  "userLevel": 3,
  "userCity": "杭州",
  "productId": "P2005",
  "productName": "华为无线耳机 FreeBuds Pro",
  "category": "数码产品",
  "brand": "华为",
  "productNum": 2,
  "productPrice": 159.9,
  "detailAmount": 319.8,
  "orderStatus": 2,
  "createTime": "2025-11-16 09:28:00"
}
```

## 配置参数说明

### 数据生成器配置 (generator.properties)
- `data.total.count`: 总共生成的订单数 (默认10000)
- `data.rate.per.second`: 每秒生成订单数 (默认10)
- `data.duplicate.rate`: 重复数据比例 (默认0.05, 即5%)
- `data.invalid.rate`: 无效数据比例 (默认0.03, 即3%)

### Flink作业配置
- **并行度**: 3 (与Kafka分区数一致)
- **Checkpoint间隔**: 60秒
- **状态后端**: RocksDB
- **水位线延迟**: 5秒

## 监控与告警

### Flink Web UI
访问 http://master1:8081 查看Flink作业运行状态

### Grafana监控看板
- **实时交易大屏**: 订单量、交易额趋势，分类Top10等
- **商品分析看板**: 分类销量趋势，品牌排行等
- **用户分析看板**: 用户等级分布，城市热力图等

## 故障排查

### 常见问题

**Q1: Kafka消费者无法连接?**
```bash
# 检查Kafka服务状态
systemctl status kafka

# 检查Topic是否存在
kafka-topics.sh --list --bootstrap-server slave1:9092
```

**Q2: Flink作业失败?**
```bash
# 查看Flink日志
tail -f /opt/flink-1.19.3/log/flink-*-taskexecutor-*.log

# 查看作业状态
flink list -t yarn-per-job
```

**Q3: ClickHouse无数据?**
```sql
-- 检查表是否存在
SHOW TABLES FROM flink_metrics;

-- 检查数据量
SELECT COUNT(*) FROM flink_metrics.metrics_time_window;
```

## 性能指标

### 预期性能
- **数据生成**: 支持每秒100条订单
- **Flink处理**: 每秒300条消息，端到端延迟 < 5秒
- **ClickHouse查询**: 简单查询 < 100ms，复杂聚合 < 1秒

### 资源要求
- **TaskManager内存**: 2048MB
- **JobManager内存**: 1024MB
- **Task Slots**: 3

## 团队分工

| 成员 | 负责模块 | 技术难度 | 开发周期 |
|------|----------|----------|----------|
| 黄 | 数据生成模块 | ★★☆☆☆ | 4天 |
| 曾 | MySQL维度表准备 | ★★☆☆☆ | 3天 |
| 林 | Flink实时清洗与维度关联 | ★★★★☆ | 6天 |
| 杨 | Flink实时指标聚合 | ★★★★☆ | 6天 |
| 李 | ClickHouse存储与Grafana可视化 | ★★★☆☆ | 6天 |

## 项目时间线

- **11月16-17日**: 环境搭建、基础框架开发
- **11月18-19日**: 核心逻辑实现
- **11月20-21日**: 功能完善、性能优化
- **11月22-23日**: 联调测试、问题修复
- **11月24日**: 最终验收、项目交付

## 许可证

本项目仅供学习使用

## 联系方式

如有问题请联系项目负责人：林

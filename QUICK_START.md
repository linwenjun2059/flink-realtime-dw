# 快速开始指南

## 🚀 5分钟快速启动

### 前置条件检查
```bash
# 检查Java版本 (需要JDK 8+)
java -version

# 检查Maven (需要Maven 3.6+)
mvn -version

# 检查Kafka是否运行
kafka-topics.sh --list --bootstrap-server slave1:9092

# 检查MySQL是否运行
mysql -h master1 -u root -p -e "SELECT VERSION();"

# 检查ClickHouse是否运行
clickhouse-client --host slave1 --query "SELECT version()"

# 检查Flink是否运行
curl http://master1:8081
```

---

## 📝 步骤1: 初始化数据库 (2分钟)

### 创建MySQL维度表
```bash
cd flink-realtime-dw/mysql-dimension

# 修改密码（如果需要）
# 编辑 create_tables.sql, insert_user_data.sql, insert_product_data.sql
# 将 'your_password' 替换为实际密码

# 执行建表和插入数据
mysql -h master1 -u root -p < create_tables.sql
mysql -h master1 -u root -p < insert_user_data.sql
mysql -h master1 -u root -p < insert_product_data.sql

# 验证
mysql -h master1 -u root -p flink_realtime_dw -e \
  "SELECT COUNT(*) FROM user_info; SELECT COUNT(*) FROM product_info;"
```

期望输出：
```
COUNT(*)
100

COUNT(*)
200
```

### 创建ClickHouse表
```bash
cd ../clickhouse-grafana

# 在slave1执行建表脚本
clickhouse-client -n < /opt/test/clickhouse_init.sql

#或者远程执行
clickhouse-client -n --host slave1 < clickhouse_init.sql
# 验证
clickhouse-client --host slave1 --query "SHOW TABLES FROM flink_metrics"
```

期望输出：
```
metrics_brand_top
metrics_category_top
metrics_city
metrics_time_window
metrics_user_level
```

---

## 📝 步骤2: 创建Kafka Topics (1分钟)

```bash
# 创建订单主表Topic
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server slave1:9092 \
  --topic order-main-source \
  --partitions 3 \
  --replication-factor 2

# 创建订单明细Topic
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server slave1:9092 \
  --topic order-detail-source \
  --partitions 3 \
  --replication-factor 2

# 创建宽表Topic
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server slave1:9092 \
  --topic order-wide-topic \
  --partitions 3 \
  --replication-factor 2

# 验证
kafka-topics.sh --list --bootstrap-server slave1:9092 | grep order
```

期望输出：
```
order-detail-source
order-main-source
order-wide-topic
```

---

## 📝 步骤3: 编译项目 (2分钟)

```bash
cd flink-realtime-dw

# 编译数据生成器
echo "编译数据生成器..."
cd data-generator
mvn clean package -DskipTests
cd ..

# 编译Flink清洗作业 (需要补充完整代码)
echo "编译Flink清洗作业..."
cd flink-cleansing
# 注意：需要根据 IMPLEMENTATION_GUIDE.md 完成核心代码
mvn clean package -DskipTests
cd ..

# 编译Flink聚合作业 (需要补充完整代码)
echo "编译Flink聚合作业..."
cd flink-aggregation
# 注意：需要根据 IMPLEMENTATION_GUIDE.md 完成核心代码
mvn clean package -DskipTests
cd ..
```

---

## 📝 步骤4: 启动数据生成器 (立即启动)

```bash
cd data-generator

# 修改配置（可选）
# 编辑 src/main/resources/generator.properties
# data.total.count=10000          # 总订单数
# data.rate.per.second=10         # 每秒生成订单数

# 启动数据生成器
java -jar target/data-generator-1.0.jar

# 或使用脚本
chmod +x start.sh
./start.sh
```

期望输出：
```
[INFO] Data Generator Started
[INFO] Configuration - Total: 10000, Rate: 10/s
[INFO] Kafka Producer initialized
[INFO] [Progress] Generated: 100/10000, Messages: 350, Rate: 10/s
...
```

### 验证Kafka数据
```bash
# 打开新终端，查看订单主表数据
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-main-source --from-beginning --max-messages 5

# 查看订单明细数据
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-detail-source --from-beginning --max-messages 5
```

---

## 📝 步骤5: 提交Flink作业 (需完成代码后执行)

### 提交清洗作业
```bash
cd flink-cleansing

# 确保已完成代码实现（参考 IMPLEMENTATION_GUIDE.md）
# 修改提交脚本中的路径
chmod +x submit-cleansing-job.sh

# 提交作业
./submit-cleansing-job.sh
```

期望输出：
```
Job has been submitted with JobID xxxxxxxx
```

### 提交聚合作业
```bash
cd ../flink-aggregation

# 确保已完成代码实现
chmod +x submit-aggregation-job.sh

# 提交作业
./submit-aggregation-job.sh
```

### 查看Flink作业状态
访问 Flink Web UI: http://master1:8081

---

## 📝 步骤6: 验证数据流转

### 查看Kafka宽表数据
```bash
# 等待Flink作业处理后，查看宽表
kafka-console-consumer.sh --bootstrap-server slave1:9092 \
  --topic order-wide-topic --from-beginning --max-messages 10
```

期望看到包含用户和商品维度信息的宽表JSON数据。

### 查看ClickHouse聚合结果
```bash
clickhouse-client --host slave1
```

```sql
-- 查看时间窗口指标
SELECT * FROM flink_metrics.metrics_time_window 
ORDER BY window_start DESC LIMIT 10;

-- 查看分类Top10
SELECT * FROM flink_metrics.metrics_category_top 
WHERE window_end = (SELECT MAX(window_end) FROM flink_metrics.metrics_category_top)
ORDER BY rank;

-- 查看用户等级统计
SELECT * FROM flink_metrics.metrics_user_level 
WHERE window_end = (SELECT MAX(window_end) FROM flink_metrics.metrics_user_level)
ORDER BY user_level;
```

---

## 📝 步骤7: 配置Grafana可视化

### 安装Grafana (如果未安装)
```bash
sudo yum install -y https://dl.grafana.com/oss/release/grafana-10.2.0-1.x86_64.rpm
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

### 安装ClickHouse插件
```bash
sudo grafana-cli plugins install vertamedia-clickhouse-datasource
sudo systemctl restart grafana-server
sudo grafana-cli plugins ls
```

### 配置数据源
1. 访问 http://master1:3000
2. 登录 (默认: admin/admin)
3. Configuration → Data Sources → Add data source
4. 选择 **ClickHouse**
5. 配置:
   - Name: `ClickHouse-Metrics`
   - URL: `http://slave1:8123`
   - Database: `flink_metrics`
   - Username: `default`
   - Password: (留空)
6. 点击 **Save & Test**

### 创建第一个Panel
1. Create → Dashboard → Add new panel
2. 选择数据源: ClickHouse-Metrics
3. 输入SQL:
```sql
SELECT 
    window_start AS time,
    order_count AS "订单数"
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
  AND window_start >= now() - INTERVAL 1 HOUR
ORDER BY time
```
4. Visualization: Time series
5. 点击 **Apply**

---

## ✅ 验收检查清单

完成以上步骤后，请验证：

- [ ] MySQL中有100个用户和200件商品
- [ ] ClickHouse中有5张指标表
- [ ] Kafka中有3个Topic，都有数据流入
- [ ] 数据生成器正常运行，持续生成数据
- [ ] Flink清洗作业正常运行（Web UI可见）
- [ ] Flink聚合作业正常运行（Web UI可见）
- [ ] Kafka宽表Topic中能看到完整宽表数据
- [ ] ClickHouse各指标表中有聚合数据
- [ ] Grafana能连接ClickHouse并展示图表

---

## 🔧 常见问题快速解决

### Q1: Maven编译失败
```bash
# 清理并重新编译
mvn clean
mvn install -DskipTests -U
```

### Q2: Kafka连接失败
```bash
# 检查Kafka服务
systemctl status kafka

# 检查网络连通性
telnet slave1 9092
```

### Q3: MySQL连接被拒绝
```bash
# 检查MySQL服务
systemctl status mysqld

# 检查防火墙
firewall-cmd --zone=public --add-port=3306/tcp --permanent
firewall-cmd --reload

# 授权远程访问
mysql -u root -p
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'your_password';
FLUSH PRIVILEGES;
```

### Q4: ClickHouse无数据
```bash
# 检查Flink作业是否正常运行
flink list -t yarn-per-job

# 查看TaskManager日志
tail -f /opt/flink-1.19.3/log/*taskexecutor*.log
```

### Q5: Grafana看不到数据
- 检查时间范围是否正确
- 检查SQL是否有语法错误
- 检查ClickHouse中是否真的有数据
- 查看Grafana日志: `sudo tail -f /var/log/grafana/grafana.log`



-- =========================================
-- Row 1: KPI核心指标卡片 (4个Stat面板)
-- =========================================

-- 面板 1.1: 今日订单总数 ✅
-- 图表类型: Stat (数字卡片)
-- 刷新间隔: 5秒
SELECT SUM(order_count) AS total_orders
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
  AND toDate(window_start) = today();

-- 面板 1.2: 今日销售总额 ✅
-- 图表类型: Stat
-- 显示单位: currency (CNY)
SELECT SUM(total_amount) AS total_sales
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
  AND toDate(window_start) = today();

-- 面板 1.3: 平均客单价 ✅
-- 图表类型: Stat with sparkline
-- 显示单位: currency (CNY)
SELECT 
    AVG(avg_amount) AS avg_order_price
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
  AND toDate(window_start) = today();

-- 面板 1.4: 订单增长率 (vs昨日同期) ✅
-- 图表类型: Stat
-- 显示单位: percent
SELECT 
    if(yesterday_orders = 0, 
       NULL,  -- 昨天无数据时返回NULL，Grafana显示为 "No data"
       round((today_orders - yesterday_orders) / yesterday_orders * 100, 2)
    ) AS growth_rate
FROM (
    SELECT 
        SUM(CASE WHEN toDate(window_start) = today() THEN order_count ELSE 0 END) AS today_orders,
        SUM(CASE WHEN toDate(window_start) = today() - 1 THEN order_count ELSE 0 END) AS yesterday_orders
    FROM flink_metrics.metrics_time_window
    WHERE window_type = '1min'
      AND window_start >= today() - INTERVAL 1 DAY
);

-- =========================================
-- Row 2: 实时趋势分析 (1个双Y轴Time Series面板) ✅
-- =========================================

-- 面板 2: 订单量 & 销售额实时趋势 (双Y轴) ✅
-- 图表类型: Time series (折线图，双Y轴)
-- 配置说明:
--   1. 左Y轴: 订单数 (单位: short, 颜色: 蓝色)
--   2. 右Y轴: 销售额 (单位: CNY ¥, 颜色: 绿色)
--   3. X轴: 时间轴 (近24小时)
--   4. 解决量级差异: 订单数(100-500) vs 销售额(10000-50000)
-- Grafana配置步骤:
--   - Field 选项卡 → 选择 "订单数" → Axis placement: Left → Unit: short
--   - Field 选项卡 → 选择 "销售额(¥)" → Axis placement: Right → Unit: CNY (¥)
SELECT 
    window_start AS time,
    order_count AS "订单数",
    total_amount AS "销售额(¥)"
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
  AND $__timeFilter(window_start)  -- Grafana时间过滤器宏
ORDER BY time;

-- =========================================
-- Row 3: 品类分析 (1个Pie Chart + 1个Bar Gauge)
-- =========================================

-- 面板 3.1: 品类销售占比 (近1小时) ✅
-- 图表类型: Pie chart (饼图)
-- 显示: 百分比 + 具体数值
SELECT 
    category AS "品类",
    SUM(sales_amount) AS "销售额"
FROM flink_metrics.metrics_category_top
WHERE window_start >= now() - INTERVAL 1 HOUR
GROUP BY category
ORDER BY "销售额" DESC;

-- 面板 3.2: Top10品类销售排行 (双Y轴) ✅
-- 图表类型: Bar chart (柱状图，支持双Y轴)
-- 配置说明:
--   1. 左Y轴: 销售额 (单位: CNY ¥, 刻度: 0-max)
--   2. 右Y轴: 销量 (单位: short, 刻度: 0-max)
--   3. X轴: 品类名称
--   4. 柱状图样式: 并排显示 (销售额用蓝色, 销量用绿色)
SELECT 
    category AS "品类",
    sales_amount AS "销售额 (¥)",
    sales_count AS "销量 (件)"
FROM flink_metrics.metrics_category_top
WHERE window_end = (
    SELECT MAX(window_end) 
    FROM flink_metrics.metrics_category_top
)
ORDER BY rank
LIMIT 10;

-- Grafana配置步骤:
-- 1. Visualization: Bar chart
-- 2. Transform: 不需要
-- 3. Field overrides:
--    Override 1: Fields with name "销售额 (¥)"
--      - Axis placement: Left
--      - Unit: currency - Chinese Yuan (CNY)
--      - Color scheme: Blue
--      - Display name: 销售额
--    Override 2: Fields with name "销量 (件)"
--      - Axis placement: Right
--      - Unit: short
--      - Color scheme: Green
--      - Display name: 销量
-- 4. Bar chart options:
--    - Orientation: Horizontal (横向柱状图)
--    - Group width: 0.7
--    - Bar width: 0.8
--    - Show values: On hover
-- 5. Legend:
--    - Mode: List
--    - Placement: Bottom

-- =========================================
-- Row 4: 用户等级分析 (1个Bar Chart - 双Y轴)
-- =========================================

-- 面板 4.1: 用户等级消费分析 ✅
-- 图表类型: Bar chart (柱状图)
-- ⚠️ 关键: 使用双Y轴解决量级差异
-- 左Y轴: 订单数 (整数, 0-500)
-- 右Y轴: 销售额 (元, 0-100000)
SELECT 
    CONCAT('Level ', toString(user_level)) AS "用户等级",
    order_count AS "订单数",
    total_amount AS "销售额(元)",
    ROUND(avg_amount_per_user, 2) AS "人均消费"
FROM flink_metrics.metrics_user_level
WHERE window_end = (
    SELECT MAX(window_end) 
    FROM flink_metrics.metrics_user_level
)
ORDER BY user_level;

-- Grafana配置提示:
-- 1. 在 Field 选项卡中:
--    - 「订单数」设置为左Y轴 (Y-axis: Left)
--    - 「销售额」设置为右Y轴 (Y-axis: Right)
-- 2. 在 Graph styles 选项卡中:
--    - Display style: Bars
--    - Bar alignment: Left

-- =========================================
-- Row 5: 地域&品牌排行 (2个Bar Gauge)
-- =========================================

-- 面板 5.1: Top15城市销售排行 ✅
-- 图表类型: Bar gauge (横向条形图)
-- 显示单位: currency (CNY)
SELECT 
    city AS "城市",
    SUM(total_amount) AS "销售额(元)",
    SUM(order_count) AS "订单数"
FROM flink_metrics.metrics_city
WHERE window_start >= now() - INTERVAL 6 HOUR
GROUP BY city
ORDER BY "销售额(元)" DESC
LIMIT 15;

-- 面板 5.2: Top10品牌销售排行 ✅
-- 图表类型: Bar gauge (横向条形图)
-- 显示单位: currency (CNY)
SELECT 
    brand AS "品牌",
    sales_amount AS "销售额(元)",
    sales_count AS "销量"
FROM flink_metrics.metrics_brand_top
WHERE window_end = (
    SELECT MAX(window_end) 
    FROM flink_metrics.metrics_brand_top
)
ORDER BY rank
LIMIT 10;

-- =========================================
-- Row 6: 实时明细监控 (1个Table)
-- =========================================

-- 面板 6.1: 实时明细数据 (最近20条) ✅
-- 图表类型: Table (表格)
-- 排序: 按时间降序
SELECT 
    formatDateTime(window_start, '%Y-%m-%d %H:%M:%S') AS "时间窗口",
    order_count AS "订单数",
    ROUND(total_amount, 2) AS "销售额(元)",
    ROUND(avg_amount, 2) AS "平均客单价(元)",
    ROUND(max_amount, 2) AS "最大订单额(元)"
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
ORDER BY window_start DESC
LIMIT 20;

-- =========================================
-- 附加查询: 辅助分析
-- =========================================

-- 辅助查询 1: 实时订单速率 (每分钟订单数)
-- 用于告警配置
SELECT 
    order_count AS current_order_rate
FROM flink_metrics.metrics_time_window
WHERE window_type = '1min'
ORDER BY window_start DESC
LIMIT 1;

-- 辅助查询 2: 销售额时段对比 (本小时 vs 上小时)
SELECT 
    ROUND((current_hour - last_hour) / last_hour * 100, 2) AS growth_rate_pct
FROM (
    SELECT 
        SUM(CASE WHEN window_start >= toStartOfHour(now()) THEN total_amount ELSE 0 END) AS current_hour,
        SUM(CASE WHEN window_start >= toStartOfHour(now()) - INTERVAL 1 HOUR 
                  AND window_start < toStartOfHour(now()) 
                 THEN total_amount ELSE 0 END) AS last_hour
    FROM flink_metrics.metrics_time_window
    WHERE window_type = '1min'
      AND window_start >= toStartOfHour(now()) - INTERVAL 1 HOUR
);

-- =========================================
-- 数据质量检查与监控
-- =========================================

-- 检查 1: 验证各表数据新鲜度 ✅
-- 用途: 监控数据是否实时更新
SELECT 
    'time_window' AS table_name,
    MAX(window_start) AS latest_time,
    COUNT(*) AS total_rows,
    dateDiff('second', MAX(window_start), now()) AS delay_seconds
FROM flink_metrics.metrics_time_window
UNION ALL
SELECT 
    'category_top',
    MAX(window_start),
    COUNT(*),
    dateDiff('second', MAX(window_start), now())
FROM flink_metrics.metrics_category_top
UNION ALL
SELECT 
    'user_level',
    MAX(window_start),
    COUNT(*),
    dateDiff('second', MAX(window_start), now())
FROM flink_metrics.metrics_user_level
UNION ALL
SELECT 
    'city',
    MAX(window_start),
    COUNT(*),
    dateDiff('second', MAX(window_start), now())
FROM flink_metrics.metrics_city
UNION ALL
SELECT 
    'brand_top',
    MAX(window_start),
    COUNT(*),
    dateDiff('second', MAX(window_start), now())
FROM flink_metrics.metrics_brand_top;

-- 检查 2: 近7天数据量趋势 ✅
-- 用途: 识别数据波动异常
SELECT 
    toDate(window_start) AS date,
    window_type,
    COUNT(*) AS record_count,
    SUM(order_count) AS total_orders,
    ROUND(SUM(total_amount), 2) AS total_sales
FROM flink_metrics.metrics_time_window
WHERE window_start >= today() - INTERVAL 7 DAY
GROUP BY date, window_type
ORDER BY date DESC, window_type;

-- =========================================
-- Grafana变量与宏使用说明
-- =========================================

-- 说明: Grafana提供强大的变量和宏功能，简化查询
-- 
-- 常用宏:
-- 1. $__timeFilter(column)   - 自动时间范围过滤
-- 2. $__timeGroup(column, interval) - 时间分组聚合
-- 3. $__interval              - 自动计算合适的时间间隔
--
-- 使用示例:
-- 
-- SELECT 
--     $__timeGroup(window_start, $__interval) AS time,
--     AVG(order_count) AS avg_orders
-- FROM flink_metrics.metrics_time_window
-- WHERE $__timeFilter(window_start)
-- GROUP BY time
-- ORDER BY time;

-- =========================================
-- 常见问题解决方案
-- =========================================

-- Q1: 为什么订单数和销售额不能放在同一个图?
-- A1: 因为数量级差异100倍以上，订单数(100)会被压成一条直线看不见
--     解决方案: 
--     - 方案1: 分成2个图表分别展示 ✅ (推荐)
--     - 方案2: 使用双Y轴 (左订单数,右销售额)
--
-- Q2: 如何设置自动刷新?
-- A2: Grafana右上角 -> Refresh interval -> 选择5s/10s/30s
--
-- Q3: 如何添加告警?
-- A3: 面板 -> Alert -> Create alert rule
--     示例: 订单数 < 10 持续5分钟 -> 发送告警
--
-- Q4: 为什么数据有延迟?
-- A4: 正常延迟 = 窗口时间 + 5秒批量写入
--     1分钟窗口: 预期延迟60-70秒
--     5分钟窗口: 预期延迟300-310秒

-- ========================================= 
-- 📋 查询索引 (快速查找)
-- =========================================
-- Row 1: 面板1.1-1.4 (行11-48)   - KPI卡片
-- Row 2: 面板2.1-2.2 (行54-75)   - 趋势图 (分开显示!)
-- Row 3: 面板3.1-3.2 (行81-105)  - 品类分析
-- Row 4: 面板4.1 (行111-134)     - 用户分析 (双Y轴!)
-- Row 5: 面板5.1-5.2 (行140-166) - 地域品牌排行
-- Row 6: 面板6.1 (行172-184)     - 实时明细表格
-- 辅助查询 (行190-211)            - 告警配置
-- 数据检查 (行217-266)            - 数据质量监控

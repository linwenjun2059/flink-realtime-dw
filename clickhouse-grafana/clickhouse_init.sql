
-- 创建数据库
CREATE DATABASE IF NOT EXISTS flink_metrics;

USE flink_metrics;

-- =========================================
-- 表1: 时间窗口指标
-- 目的: 存储按时间窗口聚合的实时交易指标 (1分钟/5分钟/1小时)
-- =========================================
DROP TABLE IF EXISTS metrics_time_window;

CREATE TABLE metrics_time_window (
    window_type String COMMENT '窗口类型: 1min/5min/1hour',
    window_start DateTime COMMENT '窗口开始时间',
    window_end DateTime COMMENT '窗口结束时间',
    order_count UInt64 COMMENT '订单数量',
    total_amount Decimal(18,2) COMMENT '交易总额',
    avg_amount Decimal(10,2) COMMENT '订单平均金额',
    max_amount Decimal(10,2) COMMENT '单笔最大金额',
    create_time DateTime DEFAULT now() COMMENT '记录创建时间'
) ENGINE = MergeTree()
ORDER BY (window_type, window_start)
PARTITION BY toYYYYMMDD(window_start)
COMMENT '时间窗口聚合指标表';

-- =========================================
-- 表2: 分类Top指标
-- 目的: 存储商品分类销量Top10
-- =========================================
DROP TABLE IF EXISTS metrics_category_top;

CREATE TABLE metrics_category_top (
    window_start DateTime COMMENT '窗口开始时间',
    window_end DateTime COMMENT '窗口结束时间',
    rank UInt8 COMMENT '排名 (1-10)',
    category String COMMENT '商品分类',
    sales_count UInt64 COMMENT '销售数量',
    sales_amount Decimal(18,2) COMMENT '销售金额',
    order_count UInt64 COMMENT '订单数量',
    create_time DateTime DEFAULT now() COMMENT '记录创建时间'
) ENGINE = MergeTree()
ORDER BY (window_start, rank)
PARTITION BY toYYYYMMDD(window_start)
COMMENT '商品分类Top10销售指标表';

-- =========================================
-- 表3: 用户等级指标
-- 目的: 存储按用户等级统计的消费数据
-- =========================================
DROP TABLE IF EXISTS metrics_user_level;

CREATE TABLE metrics_user_level (
    window_start DateTime COMMENT '窗口开始时间',
    window_end DateTime COMMENT '窗口结束时间',
    user_level UInt8 COMMENT '用户等级 (1-5)',
    user_count UInt64 COMMENT '下单用户数',
    order_count UInt64 COMMENT '订单数量',
    total_amount Decimal(18,2) COMMENT '消费总额',
    avg_amount_per_user Decimal(10,2) COMMENT '人均消费金额',
    create_time DateTime DEFAULT now() COMMENT '记录创建时间'
) ENGINE = MergeTree()
ORDER BY (window_start, user_level)
PARTITION BY toYYYYMMDD(window_start)
COMMENT '用户等级消费指标表';

-- =========================================
-- 表4: 城市指标
-- 目的: 存储按城市统计的订单分布
-- =========================================
DROP TABLE IF EXISTS metrics_city;

CREATE TABLE metrics_city (
    window_start DateTime COMMENT '窗口开始时间',
    window_end DateTime COMMENT '窗口结束时间',
    city String COMMENT '城市名称',
    order_count UInt64 COMMENT '订单数量',
    total_amount Decimal(18,2) COMMENT '交易总额',
    create_time DateTime DEFAULT now() COMMENT '记录创建时间'
) ENGINE = MergeTree()
ORDER BY (window_start, city)
PARTITION BY toYYYYMMDD(window_start)
COMMENT '城市订单分布指标表';

-- =========================================
-- 表5: 品牌Top指标
-- 目的: 存储品牌销量Top20
-- =========================================
DROP TABLE IF EXISTS metrics_brand_top;

CREATE TABLE metrics_brand_top (
    window_start DateTime COMMENT '窗口开始时间',
    window_end DateTime COMMENT '窗口结束时间',
    rank UInt8 COMMENT '排名 (1-20)',
    brand String COMMENT '品牌名称',
    sales_count UInt64 COMMENT '销售数量',
    sales_amount Decimal(18,2) COMMENT '销售金额',
    create_time DateTime DEFAULT now() COMMENT '记录创建时间'
) ENGINE = MergeTree()
ORDER BY (window_start, rank)
PARTITION BY toYYYYMMDD(window_start)
COMMENT '品牌Top20销售指标表';

-- =========================================
-- 验证查询
-- =========================================

-- 显示所有表
SHOW TABLES;

-- 显示表结构
DESCRIBE TABLE metrics_time_window;
DESCRIBE TABLE metrics_category_top;
DESCRIBE TABLE metrics_user_level;
DESCRIBE TABLE metrics_city;
DESCRIBE TABLE metrics_brand_top;

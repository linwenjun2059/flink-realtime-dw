

-- 创建数据库
CREATE DATABASE IF NOT EXISTS flink_realtime_dw 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_general_ci;

USE flink_realtime_dw;

-- =========================================
-- 用户维度表
-- =========================================
DROP TABLE IF EXISTS `user_info`;

CREATE TABLE `user_info` (
  `user_id` VARCHAR(20) NOT NULL COMMENT '用户ID，格式：U+4位数字，例如 U1001',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户姓名',
  `user_level` INT NOT NULL COMMENT '用户等级：1-普通 2-白银 3-黄金 4-铂金 5-钻石',
  `register_time` DATETIME NOT NULL COMMENT '注册时间',
  `city` VARCHAR(30) NOT NULL COMMENT '所在城市',
  PRIMARY KEY (`user_id`),
  KEY `idx_city` (`city`),
  KEY `idx_level` (`user_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户维度表';

-- =========================================
-- 商品维度表
-- =========================================
DROP TABLE IF EXISTS `product_info`;

CREATE TABLE `product_info` (
  `product_id` VARCHAR(20) NOT NULL COMMENT '商品ID，格式：P+4位数字，例如 P2001',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `category` VARCHAR(50) NOT NULL COMMENT '商品分类',
  `brand` VARCHAR(50) NOT NULL COMMENT '品牌',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品原价',
  PRIMARY KEY (`product_id`),
  KEY `idx_category` (`category`),
  KEY `idx_brand` (`brand`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品维度表';

-- 验证表是否创建成功
SHOW TABLES;

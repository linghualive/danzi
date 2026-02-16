-- 创建 base_db 数据库
CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建 mall_db 数据库
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- base_db 表结构
USE base_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密',
    nickname    VARCHAR(50)  NOT NULL,
    avatar      VARCHAR(255) DEFAULT NULL,
    role        INT          NOT NULL DEFAULT 0 COMMENT '0-普通用户 1-主播 2-管理员',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 直播间表
CREATE TABLE IF NOT EXISTS live_room (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '主播用户ID',
    title       VARCHAR(100) NOT NULL,
    cover       VARCHAR(255) DEFAULT NULL,
    status      INT          NOT NULL DEFAULT 0 COMMENT '0-未开播 1-直播中 2-已结束',
    stream_key  VARCHAR(64)  NOT NULL UNIQUE COMMENT 'OBS 推流密钥',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);

-- mall_db 表结构
USE mall_db;

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT         DEFAULT NULL COMMENT '关联直播间ID',
    name        VARCHAR(100)   NOT NULL,
    description TEXT           DEFAULT NULL,
    price       DECIMAL(10,2)  NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    image       VARCHAR(255)   DEFAULT NULL,
    status      INT            NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
);

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no     VARCHAR(32)    NOT NULL UNIQUE COMMENT '订单编号',
    user_id      BIGINT         NOT NULL,
    total_amount DECIMAL(10,2)  NOT NULL,
    status       INT            NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no)
);

-- 订单项表
CREATE TABLE IF NOT EXISTS order_item (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(100)   NOT NULL,
    price        DECIMAL(10,2)  NOT NULL,
    quantity     INT            NOT NULL,
    INDEX idx_order_id (order_id)
);

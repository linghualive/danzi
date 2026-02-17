-- 清空项目数据库数据（base_db + mall_db）
-- 使用前请确认服务已至少启动过一次，表结构已由 JPA 创建

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================
-- base_db
-- =============================
USE base_db;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE private_message;
TRUNCATE TABLE user_follow;
TRUNCATE TABLE room_warning;
TRUNCATE TABLE chat_message;
TRUNCATE TABLE live_room;
TRUNCATE TABLE user_profile;
TRUNCATE TABLE media_file;
TRUNCATE TABLE `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================
-- mall_db
-- =============================
USE mall_db;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE order_item;
TRUNCATE TABLE `order`;
TRUNCATE TABLE product;
TRUNCATE TABLE media_file;

SET FOREIGN_KEY_CHECKS = 1;

-- Clear project databases data (base_db + mall_db)
-- Ensure services have started at least once so tables exist

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- base_db
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

-- mall_db
USE mall_db;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE order_item;
TRUNCATE TABLE `order`;
TRUNCATE TABLE product;
TRUNCATE TABLE media_file;

SET FOREIGN_KEY_CHECKS = 1;

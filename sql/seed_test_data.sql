-- Seed test data for base_db + mall_db
-- Run sql/clear_all_db.sql first

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- base_db seed
USE base_db;
SET FOREIGN_KEY_CHECKS = 0;

-- Backward compatibility for new base_db columns
SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `user` ADD COLUMN `status` INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'live_room' AND COLUMN_NAME = 'started_at'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `live_room` ADD COLUMN `started_at` DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'live_room' AND COLUMN_NAME = 'stopped_at'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `live_room` ADD COLUMN `stopped_at` DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Users (password hash corresponds to password123)
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `role`, `status`, `created_at`, `updated_at`) VALUES
(1, 'anchor1', '$2a$10$EaZ.k6nouTdaaUjwSSzw.OX8WYg66GOuZ3UJ0AAyVJVbAINL9nVLe', 'Anchor Amy', '/api/base/media/public/1', 1, 0, NOW(), NOW()),
(2, 'anchor2', '$2a$10$EaZ.k6nouTdaaUjwSSzw.OX8WYg66GOuZ3UJ0AAyVJVbAINL9nVLe', 'Anchor Bob', '/api/base/media/public/2', 1, 0, NOW(), NOW()),
(3, 'admin',   '$2a$10$EaZ.k6nouTdaaUjwSSzw.OX8WYg66GOuZ3UJ0AAyVJVbAINL9nVLe', 'Platform Admin', '/api/base/media/public/3', 2, 0, NOW(), NOW()),
(4, 'buyer1',  '$2a$10$EaZ.k6nouTdaaUjwSSzw.OX8WYg66GOuZ3UJ0AAyVJVbAINL9nVLe', 'Buyer Alice', NULL, 0, 0, NOW(), NOW()),
(5, 'buyer2',  '$2a$10$EaZ.k6nouTdaaUjwSSzw.OX8WYg66GOuZ3UJ0AAyVJVbAINL9nVLe', 'Buyer David', NULL, 0, 0, NOW(), NOW());

-- Base media placeholder
INSERT INTO media_file (`id`, `file_name`, `content_type`, `file_size`, `data`, `created_by`, `created_at`) VALUES
(1, 'avatar_anchor1.png', 'image/png', 4, X'89504E47', 1, NOW()),
(2, 'avatar_anchor2.png', 'image/png', 4, X'89504E47', 2, NOW()),
(3, 'avatar_admin.png',   'image/png', 4, X'89504E47', 3, NOW());

-- Profiles
INSERT INTO user_profile (`user_id`, `bio`, `avatar_file_id`, `updated_at`) VALUES
(1, 'Beauty product reviews and skincare tips.', 1, NOW()),
(2, 'Gadget unboxing and technical reviews.', 2, NOW()),
(3, 'Platform moderation and risk control.', 3, NOW()),
(4, 'Frequent live shopping customer.', NULL, NOW()),
(5, 'Interested in 3C and home goods.', NULL, NOW());

-- Broadcast qualifications (anchors approved)
INSERT INTO broadcast_qualification (`id`, `user_id`, `contact_info`, `business_license`, `personal_info`, `status`, `reject_reason`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES
(1, 1, '13800000001', 'BL-2026-001', 'Beauty anchor with 3 years of experience', 1, NULL, 3, NOW(), NOW(), NOW()),
(2, 2, '13800000002', 'BL-2026-002', 'Tech reviewer with gadget expertise', 1, NULL, 3, NOW(), NOW(), NOW());

-- Live rooms
INSERT INTO live_room (`id`, `user_id`, `title`, `cover`, `cover_file_id`, `status`, `stream_key`, `closed_reason`, `closed_by`, `closed_at`, `created_at`, `updated_at`) VALUES
(1, 1, 'Amy Beauty Live', 'https://example.com/cover1.jpg', NULL, 1, 'stream_key_room_001', NULL, NULL, NULL, NOW(), NOW()),
(2, 2, 'Bob Gadget Live', 'https://example.com/cover2.jpg', NULL, 1, 'stream_key_room_002', NULL, NULL, NULL, NOW(), NOW()),
(3, 1, 'Policy Violation Demo Room', NULL, NULL, 3, 'stream_key_room_003', 'Policy violation, closed by admin', 3, NOW(), NOW(), NOW());

-- Follow relationships
INSERT INTO user_follow (`id`, `follower_id`, `followee_id`, `created_at`) VALUES
(1, 4, 1, NOW()),
(2, 5, 1, NOW()),
(3, 4, 2, NOW());

-- Private messages
INSERT INTO private_message (`id`, `sender_id`, `receiver_id`, `content`, `is_read`, `created_at`) VALUES
(1, 4, 1, 'Hi, is this product suitable for sensitive skin?', 0, NOW()),
(2, 1, 4, 'Yes, we will explain ingredients in detail tonight.', 0, NOW()),
(3, 5, 2, 'Does the charger support laptop fast charging?', 1, NOW());

-- Room warning logs
INSERT INTO room_warning (`id`, `room_id`, `admin_id`, `message`, `created_at`) VALUES
(1, 3, 3, 'Please fix policy issues immediately.', NOW());

-- Chat messages
INSERT INTO chat_message (`id`, `room_id`, `user_id`, `nickname`, `type`, `content`, `created_at`) VALUES
(1, 1, 4, 'Buyer Alice', 'COMMENT', 'Any promo today?', NOW()),
(2, 1, 1, 'Anchor Amy', 'COMMENT', 'Coupons will be posted shortly.', NOW()),
(3, 2, 5, 'Buyer David', 'DANMAKU', 'This keyboard looks great!', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- mall_db schema patch + seed
USE mall_db;
SET FOREIGN_KEY_CHECKS = 0;

-- Backward compatibility for old schemas (compatible with MySQL versions
-- that do not support "ADD COLUMN IF NOT EXISTS")
SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'seller_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order` ADD COLUMN `seller_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'expire_at'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order` ADD COLUMN `expire_at` DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'paid_at'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order` ADD COLUMN `paid_at` DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_reason'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order` ADD COLUMN `refund_reason` VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_requested_at'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order` ADD COLUMN `refund_requested_at` DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'room_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `product` ADD COLUMN `room_id` BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'seller_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `product` ADD COLUMN `seller_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'image_file_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `product` ADD COLUMN `image_file_id` BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'status'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `product` ADD COLUMN `status` INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND COLUMN_NAME = 'product_image'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE `order_item` ADD COLUMN `product_image` VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Product media
INSERT INTO media_file (`id`, `file_name`, `content_type`, `file_size`, `data`, `created_by`, `created_at`) VALUES
(1, 'product_beauty_1.png', 'image/png', 4, X'89504E47', 1, NOW()),
(2, 'product_digital_1.png', 'image/png', 4, X'89504E47', 2, NOW());

-- Products
INSERT INTO product (`id`, `room_id`, `seller_id`, `name`, `description`, `price`, `stock`, `image`, `image_file_id`, `status`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'Lancome Serum 50ml', 'Skin repair serum', 899.00, 200, '/api/product/media/public/1', 1, 1, NOW(), NOW()),
(2, 1, 1, 'Estee Foundation 30ml', 'Long-wear watery glow finish', 380.00, 150, NULL, NULL, 1, NOW(), NOW()),
(3, 1, 1, 'MAC Lipstick 646', 'Popular daily shade', 170.00, 500, NULL, NULL, 1, NOW(), NOW()),
(4, 2, 2, 'Apple AirPods Pro 2', 'ANC with adaptive transparency', 1799.00, 80, '/api/product/media/public/2', 2, 1, NOW(), NOW()),
(5, 2, 2, 'Anker 65W GaN Charger', 'Triple-port fast charger', 189.00, 400, NULL, NULL, 1, NOW(), NOW()),
(6, NULL, 2, 'Desk Organizer Box', 'Multi-compartment organizer', 39.90, 800, NULL, NULL, 1, NOW(), NOW());

-- Orders
-- status: 0=pending 1=paid 2=canceled 3=refund-requested 4=refunded
INSERT INTO `order` (`id`, `order_no`, `user_id`, `seller_id`, `total_amount`, `status`, `expire_at`, `paid_at`, `refund_reason`, `refund_requested_at`, `created_at`, `updated_at`) VALUES
(1, '2026021700010001', 4, 1, 899.00, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL, NULL, NULL, NOW(), NOW()),
(2, '2026021700010002', 5, 1, 760.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE), NOW()),
(3, '2026021700010003', 4, 2, 189.00, 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 MINUTE), INTERVAL 5 MINUTE), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NOW()),
(4, '2026021700010004', 5, 2, 1799.00, 3, DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 29 MINUTE), 'Appearance defect', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW()),
(5, '2026021700010005', 4, 1, 170.00, 4, DATE_ADD(DATE_SUB(NOW(), INTERVAL 40 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 39 MINUTE), 'Changed mind', DATE_SUB(NOW(), INTERVAL 35 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), NOW()),
(6, '2026021700010006', 5, 2, 378.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 12 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), NOW());

-- Order items
INSERT INTO order_item (`id`, `order_id`, `product_id`, `product_name`, `price`, `quantity`) VALUES
(1, 1, 1, 'Lancome Serum 50ml', 899.00, 1),
(2, 2, 2, 'Estee Foundation 30ml', 380.00, 2),
(3, 3, 5, 'Anker 65W GaN Charger', 189.00, 1),
(4, 4, 4, 'Apple AirPods Pro 2', 1799.00, 1),
(5, 5, 3, 'MAC Lipstick 646', 170.00, 1),
(6, 6, 5, 'Anker 65W GaN Charger', 189.00, 2);

-- Fill expire_at for old rows
UPDATE `order`
SET `expire_at` = DATE_ADD(`created_at`, INTERVAL 5 MINUTE)
WHERE `expire_at` IS NULL;

ALTER TABLE `order` MODIFY COLUMN `expire_at` DATETIME NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- 补齐测试数据（base_db + mall_db）
-- 建议先执行 sql/clear_all_db.sql
-- 如字段缺失（历史库结构），本脚本会尽量补齐关键字段

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ======================================
-- base_db: seed
-- ======================================
USE base_db;
SET FOREIGN_KEY_CHECKS = 0;

-- 用户（密码均为 password123 的 bcrypt）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `role`, `created_at`, `updated_at`) VALUES
(1, 'anchor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '美妆主播小美', '/api/base/media/public/1', 1, NOW(), NOW()),
(2, 'anchor2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '数码达人老王', '/api/base/media/public/2', 1, NOW(), NOW()),
(3, 'admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '平台管理员', '/api/base/media/public/3', 2, NOW(), NOW()),
(4, 'buyer1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '爱逛直播的小张', NULL, 0, NOW(), NOW()),
(5, 'buyer2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '理性购物小李', NULL, 0, NOW(), NOW());

-- 基础媒体（占位二进制）
INSERT INTO media_file (`id`, `file_name`, `content_type`, `file_size`, `data`, `created_by`, `created_at`) VALUES
(1, 'avatar_anchor1.png', 'image/png', 4, X'89504E47', 1, NOW()),
(2, 'avatar_anchor2.png', 'image/png', 4, X'89504E47', 2, NOW()),
(3, 'avatar_admin.png',   'image/png', 4, X'89504E47', 3, NOW());

-- 个人资料
INSERT INTO user_profile (`user_id`, `bio`, `avatar_file_id`, `updated_at`) VALUES
(1, '专注美妆测评与护肤干货分享。', 1, NOW()),
(2, '数码开箱与实测，理性种草。', 2, NOW()),
(3, '平台内容治理与风险控制。', 3, NOW()),
(4, '经常在直播间买护肤和零食。', NULL, NOW()),
(5, '偏爱3C和家居用品。', NULL, NOW());

-- 直播间
INSERT INTO live_room (`id`, `user_id`, `title`, `cover`, `cover_file_id`, `status`, `stream_key`, `closed_reason`, `closed_by`, `closed_at`, `created_at`, `updated_at`) VALUES
(1, 1, '小美的美妆直播间', 'https://example.com/cover1.jpg', NULL, 1, 'stream_key_room_001', NULL, NULL, NULL, NOW(), NOW()),
(2, 2, '老王的数码好物分享', 'https://example.com/cover2.jpg', NULL, 1, 'stream_key_room_002', NULL, NULL, NULL, NOW(), NOW()),
(3, 1, '违规演示直播间（示例）', NULL, NULL, 3, 'stream_key_room_003', '违规内容，已关闭', 3, NOW(), NOW(), NOW());

-- 关注关系
INSERT INTO user_follow (`id`, `follower_id`, `followee_id`, `created_at`) VALUES
(1, 4, 1, NOW()),
(2, 5, 1, NOW()),
(3, 4, 2, NOW());

-- 私信
INSERT INTO private_message (`id`, `sender_id`, `receiver_id`, `content`, `is_read`, `created_at`) VALUES
(1, 4, 1, '主播你好，这款精华适合敏感肌吗？', 0, NOW()),
(2, 1, 4, '可以的，今晚会详细讲成分。', 0, NOW()),
(3, 5, 2, '充电器支持笔记本快充吗？', 1, NOW());

-- 直播间警告记录
INSERT INTO room_warning (`id`, `room_id`, `admin_id`, `message`, `created_at`) VALUES
(1, 3, 3, '请立即整改违规内容', NOW());

-- 聊天消息
INSERT INTO chat_message (`id`, `room_id`, `user_id`, `nickname`, `type`, `content`, `created_at`) VALUES
(1, 1, 4, '爱逛直播的小张', 'COMMENT', '主播今天有什么福利？', NOW()),
(2, 1, 1, '美妆主播小美', 'COMMENT', '稍后上链接，有限时券。', NOW()),
(3, 2, 5, '理性购物小李', 'DANMAKU', '这个键盘手感不错！', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ======================================
-- mall_db: schema patch + seed
-- ======================================
USE mall_db;
SET FOREIGN_KEY_CHECKS = 0;

-- 历史库结构兼容（补齐新字段）
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `seller_id` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `expire_at` DATETIME NULL;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `paid_at` DATETIME NULL;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `refund_reason` VARCHAR(255) NULL;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `refund_requested_at` DATETIME NULL;

ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `room_id` BIGINT NULL;
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `seller_id` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `image_file_id` BIGINT NULL;
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `status` INT NOT NULL DEFAULT 1;

-- 商品媒体
INSERT INTO media_file (`id`, `file_name`, `content_type`, `file_size`, `data`, `created_by`, `created_at`) VALUES
(1, 'product_beauty_1.png', 'image/png', 4, X'89504E47', 1, NOW()),
(2, 'product_digital_1.png', 'image/png', 4, X'89504E47', 2, NOW());

-- 商品
INSERT INTO product (`id`, `room_id`, `seller_id`, `name`, `description`, `price`, `stock`, `image`, `image_file_id`, `status`, `created_at`, `updated_at`) VALUES
(1, 1, 1, '兰蔻小黑瓶精华液 50ml', '肌底修护精华，改善肌肤状态', 899.00, 200, '/api/product/media/public/1', 1, 1, NOW(), NOW()),
(2, 1, 1, '雅诗兰黛沁水粉底液 30ml', '持妆一整天，自然水光肌', 380.00, 150, NULL, NULL, 1, NOW(), NOW()),
(3, 1, 1, 'MAC子弹头口红 #646', '热门色号，显白日常', 170.00, 500, NULL, NULL, 1, NOW(), NOW()),
(4, 2, 2, 'Apple AirPods Pro 2', '主动降噪，自适应通透模式', 1799.00, 80, '/api/product/media/public/2', 2, 1, NOW(), NOW()),
(5, 2, 2, 'Anker 65W氮化镓充电器', '三口快充，兼容多设备', 189.00, 400, NULL, NULL, 1, NOW(), NOW()),
(6, NULL, 2, '得力办公桌面收纳盒', '多功能分格，桌面整理神器', 39.90, 800, NULL, NULL, 1, NOW(), NOW());

-- 订单（覆盖待支付/已支付/已取消/退款申请中）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `seller_id`, `total_amount`, `status`, `expire_at`, `paid_at`, `refund_reason`, `refund_requested_at`, `created_at`, `updated_at`) VALUES
(1, '2026021700010001', 4, 1, 899.00, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL, NULL, NULL, NOW(), NOW()),
(2, '2026021700010002', 5, 1, 760.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE), NOW()),
(3, '2026021700010003', 4, 2, 189.00, 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 MINUTE), INTERVAL 5 MINUTE), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NOW()),
(4, '2026021700010004', 5, 2, 1799.00, 3, DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 29 MINUTE), '商品外观有瑕疵', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW());

-- 订单项
INSERT INTO order_item (`id`, `order_id`, `product_id`, `product_name`, `price`, `quantity`) VALUES
(1, 1, 1, '兰蔻小黑瓶精华液 50ml', 899.00, 1),
(2, 2, 2, '雅诗兰黛沁水粉底液 30ml', 380.00, 2),
(3, 3, 5, 'Anker 65W氮化镓充电器', 189.00, 1),
(4, 4, 4, 'Apple AirPods Pro 2', 1799.00, 1);

-- 对历史空值兜底
UPDATE `order`
SET `expire_at` = DATE_ADD(`created_at`, INTERVAL 5 MINUTE)
WHERE `expire_at` IS NULL;

ALTER TABLE `order` MODIFY COLUMN `expire_at` DATETIME NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

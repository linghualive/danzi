-- 每次执行此脚本会清空业务数据并重新插入种子数据
-- 说明：请先启动微服务一次，让 JPA 自动建表后再执行

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================
-- 清空 base_db
-- =============================================
USE base_db;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM chat_message;
DELETE FROM live_room;
DELETE FROM `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 清空 mall_db
-- =============================================
USE mall_db;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM order_item;
DELETE FROM `order`;
DELETE FROM product;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 种子数据 - base_db
-- =============================================
USE base_db;

INSERT INTO `user` (id, username, password, nickname, avatar, role, created_at, updated_at) VALUES
(1, 'anchor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '美妆主播小美', 'https://example.com/avatar1.jpg', 1, NOW(), NOW()),
(2, 'anchor2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '数码达人老王', 'https://example.com/avatar2.jpg', 1, NOW(), NOW());

INSERT INTO live_room (id, user_id, title, cover, status, stream_key, created_at, updated_at) VALUES
(1, 1, '小美的美妆直播间', 'https://example.com/cover1.jpg', 1, 'stream_key_room_001', NOW(), NOW()),
(2, 2, '老王的数码好物分享', 'https://example.com/cover2.jpg', 1, 'stream_key_room_002', NOW(), NOW());

-- =============================================
-- 种子数据 - mall_db
-- =============================================
USE mall_db;

INSERT INTO product (id, room_id, name, description, price, stock, image, status, created_at, updated_at) VALUES
(1,  1, '兰蔻小黑瓶精华液 50ml',    '肌底修护精华，改善肌肤状态',          899.00, 200, 'https://example.com/product1.jpg', 1, NOW(), NOW()),
(2,  1, '雅诗兰黛沁水粉底液 30ml',   '持妆一整天，自然水光肌',              380.00, 150, 'https://example.com/product2.jpg', 1, NOW(), NOW()),
(3,  1, 'MAC子弹头口红 #646',        '热门色号 MARRAKESH，显白日常',         170.00, 500, 'https://example.com/product3.jpg', 1, NOW(), NOW()),
(4,  1, '珂润保湿面霜 40g',          '敏感肌专用，神经酰胺保湿',            158.00, 300, 'https://example.com/product4.jpg', 1, NOW(), NOW()),
(5,  2, 'Apple AirPods Pro 2',       '主动降噪，自适应通透模式',            1799.00,  80, 'https://example.com/product5.jpg', 1, NOW(), NOW()),
(6,  2, '小米14 Ultra 16+512G',      '徕卡光学镜头，骁龙8 Gen3',           5999.00,  50, 'https://example.com/product6.jpg', 1, NOW(), NOW()),
(7,  2, 'Anker 65W氮化镓充电器',     '三口快充，兼容多设备',                189.00, 400, 'https://example.com/product7.jpg', 1, NOW(), NOW()),
(8,  NULL, '三只松鼠坚果大礼包 1.5kg', '每日坚果混合装，8袋独立包装',        89.90, 1000, 'https://example.com/product8.jpg', 1, NOW(), NOW()),
(9,  NULL, '农夫山泉NFC橙汁 300ml*24', '100%鲜榨，非浓缩还原',              109.00,  600, 'https://example.com/product9.jpg', 1, NOW(), NOW()),
(10, NULL, '得力办公桌面收纳盒',       '多功能分格，桌面整理神器',            39.90,  800, 'https://example.com/product10.jpg', 1, NOW(), NOW());

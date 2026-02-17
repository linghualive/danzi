-- Docker 初始化脚本：仅创建数据库
-- 表结构由 JPA ddl-auto: update 自动创建
-- 种子数据通过 ./run.sh seed 手动导入

CREATE DATABASE IF NOT EXISTS base_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

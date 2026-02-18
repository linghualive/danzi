# Live Commerce - 沉浸式实时互动直播电商平台

基于 Kotlin + Spring Boot 3 + Spring Cloud 的微服务直播电商平台，支持直播推拉流、实时弹幕聊天、商品展示与下单。

## 最简单云端部署流程（4 步）

### 1) 本地打包部署文件

在项目根目录执行（Windows 请用 Git Bash / WSL）：

```bash
chmod +x run.sh
./run.sh export
```

执行后会生成：`dist/live-commerce-docker.tar.gz`

### 2) 上传到云服务器

```bash
scp dist/live-commerce-docker.tar.gz user@your-server:/opt/
```

### 3) 服务器解压并启动

```bash
ssh user@your-server
cd /opt
tar xzf live-commerce-docker.tar.gz
cd live-commerce-docker
chmod +x run.sh
./run.sh start
```

### 4) 导入测试数据并访问

```bash
./run.sh seed
```

访问：

- 前端：`http://服务器IP或域名`
- 网关 API：`http://服务器IP或域名:9000`

## 环境要求

- Docker（含 Docker Compose V2）
- Linux/macOS，或 Windows + Git Bash/WSL（用于执行 `run.sh`）

## 快速启动

### Linux / macOS

```bash
# 首次执行建议加可执行权限
chmod +x run.sh

# 构建并启动全部服务
./run.sh start

# 导入测试数据（首次启动后执行）
./run.sh seed
```

### Windows

在 Git Bash 或 WSL 中执行与 Linux 相同命令：

```bash
./run.sh start
./run.sh seed
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `run.sh start` | 构建并启动全部服务（默认命令） |
| `run.sh stop` | 停止所有容器 |
| `run.sh restart` | 重启所有容器 |
| `run.sh status` | 查看容器状态 |
| `run.sh logs` | 查看全部日志 |
| `run.sh logs gateway` | 查看指定服务日志 |
| `run.sh seed` | 清空业务数据并导入种子数据 |
| `run.sh rebuild` | 重新编译 Java 服务并重启 |
| `run.sh down` | 停止并删除容器 |
| `run.sh clean` | 删除容器和数据卷（会清空 MySQL/Redis 数据） |
| `run.sh export` | 构建镜像并导出部署包 |

## 启动后访问地址

| 入口 | 地址 |
|------|------|
| 前端页面 | http://localhost:80 |
| Gateway（统一入口） | http://localhost:9000 |
| base-service | http://localhost:9001 |
| mall-service | http://localhost:9002 |
| SRS RTMP | rtmp://localhost:1935/live/ |
| SRS HTTP-FLV | http://localhost:8080 |

## 导入测试数据与账号

`run.sh seed` 会执行：

- `sql/clear_all_db.sql`：清空 `base_db` 和 `mall_db` 业务数据
- `sql/seed_test_data.sql`：导入用户、直播间、商品、订单、消息等示例数据

测试账号密码统一为：`password123`

| 用户名 | 角色 | 说明 |
|------|------|------|
| `admin` | 管理员 | 管理用户角色、警告/关闭直播间 |
| `anchor1` | 主播 | 直播与商品示例账号 1 |
| `anchor2` | 主播 | 直播与商品示例账号 2 |
| `buyer1` | 普通用户 | 买家示例账号 1 |
| `buyer2` | 普通用户 | 买家示例账号 2 |

## 直播推流

使用 OBS 或其他推流软件：

- 服务器：`rtmp://localhost:1935/live`
- 推流密钥：进入直播间详情页底部查看 `streamKey`

## 端口与环境变量

`docker-compose.prod.yml` 支持以下常用变量覆盖：

- `MYSQL_ROOT_PASSWORD`（默认 `root123`）
- `MYSQL_PORT`（默认 `3306`）
- `REDIS_PORT`（默认 `6379`）
- `GATEWAY_PORT`（默认 `9000`）
- `FRONTEND_PORT`（默认 `80`）
- `SRS_RTMP_PORT`（默认 `1935`）
- `SRS_HTTP_PORT`（默认 `8080`）
- `SRS_API_PORT`（默认 `1985`）
- `BASE_JAVA_OPTS` / `MALL_JAVA_OPTS` / `GATEWAY_JAVA_OPTS`

示例（Linux/macOS）：

```bash
MYSQL_ROOT_PASSWORD=your_password GATEWAY_PORT=19000 ./run.sh start
```

示例（Windows Git Bash / WSL）：

```bash
MYSQL_ROOT_PASSWORD=your_password GATEWAY_PORT=19000 ./run.sh start
```

## 打包部署（推荐）

本地执行：

```bash
./run.sh export
```

会生成：`dist/live-commerce-docker.tar.gz`

服务器部署步骤：

```bash
tar xzf live-commerce-docker.tar.gz
cd live-commerce-docker
./run.sh start
./run.sh seed
```

说明：部署包内含 `images.tar`。`./run.sh start` 会自动加载该镜像包并启动。

Windows 在 Git Bash / WSL 中同样使用：

```bash
./run.sh export
```

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.2.x |
| Spring Cloud Gateway | 2023.0.x |
| Kotlin | 1.9.x |
| Sa-Token | 1.38.0 |
| SRS | v6 |
| MySQL | 8.0 |
| Redis | 7.x |
| JDK | 17 |

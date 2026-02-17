# Live Commerce - 沉浸式实时互动直播电商平台

基于 Kotlin + Spring Boot 3 + Spring Cloud 的微服务直播电商平台，支持直播推拉流、实时弹幕聊天、商品展示与下单。

## 环境要求

- JDK 17+
- Docker & Docker Compose
- Maven 3.8+（项目自带 mvnw，可不单独安装）

## 快速启动

### Linux / macOS

```bash
# 一键启动（中间件 + 编译 + 微服务）
./start.sh

# 或分步执行
./start.sh infra      # 仅启动中间件（MySQL/Redis/SRS）
./start.sh build      # 仅编译项目
./start.sh services   # 仅启动微服务（需中间件已运行）
./start.sh package    # 打包成可部署的 tar.gz

# 停止所有服务
./start.sh stop

# 查看运行状态
./start.sh status
```

### Windows

```cmd
REM 一键启动
start.bat

REM 或分步执行
start.bat infra       &REM 仅启动中间件
start.bat build       &REM 仅编译项目
start.bat services    &REM 仅启动微服务
start.bat package     &REM 打包成可部署的 zip

REM 停止所有服务
start.bat stop

REM 查看运行状态
start.bat status
```

启动完成后：

| 入口 | 地址 |
|------|------|
| Gateway（统一入口） | http://localhost:9000 |
| base-service | http://localhost:9001 |
| mall-service | http://localhost:9002 |
| 前端测试页面 | 浏览器打开 `frontend/index.html` |
| Swagger (base) | http://localhost:9001/swagger-ui.html |
| Swagger (mall) | http://localhost:9002/swagger-ui.html |

## 导入测试数据

首次启动后，表结构由 JPA 自动创建。建议用以下两个脚本：

- `sql/clear_all_db.sql`：清空 `base_db` 和 `mall_db` 业务数据
- `sql/seed_test_data.sql`：补齐测试数据（用户、直播间、商品、订单、消息等）

Linux/macOS：

```bash
docker exec -i live-commerce-mysql mysql -uroot -proot123 < sql/clear_all_db.sql
docker exec -i live-commerce-mysql mysql -uroot -proot123 < sql/seed_test_data.sql
```

Windows `cmd`：

```cmd
docker exec -i live-commerce-mysql mysql -uroot -proot123 < .\sql\clear_all_db.sql && docker exec -i live-commerce-mysql mysql -uroot -proot123 < .\sql\seed_test_data.sql
```

说明：`seed_test_data.sql` 已包含订单状态样例（待支付、已支付、已取消、退款申请中、已退款），用于联调退款流程（买家申请，卖家确认）。

### 测试账号（seed_test_data.sql）

密码统一为：`password123`

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

## 配置修改

项目中的中间件地址默认全部指向 `localhost`。如果你的 MySQL/Redis/SRS 部署在其他机器上，需要修改以下文件：

### MySQL

需要改 **2 个** 配置文件（两个服务各连各的库）：

| 文件 | 配置项 |
|------|--------|
| `base-service/src/main/resources/application.yml` | `spring.datasource.url` / `username` / `password` |
| `mall-service/src/main/resources/application.yml` | `spring.datasource.url` / `username` / `password` |

示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的MySQL地址:3306/base_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

> base-service 连 `base_db`，mall-service 连 `mall_db`，注意区分。

如果同时改了 Docker 中 MySQL 的端口映射，还需修改 `docker-compose.yml` 的 `ports` 配置。

### Redis

需要改 **3 个** 配置文件（三个服务都用 Redis 做 Sa-Token 会话共享）：

| 文件 | 配置项 |
|------|--------|
| `base-service/src/main/resources/application.yml` | `spring.data.redis.host` / `port` |
| `mall-service/src/main/resources/application.yml` | `spring.data.redis.host` / `port` |
| `gateway/src/main/resources/application.yml` | `spring.data.redis.host` / `port` |

示例：

```yaml
spring:
  data:
    redis:
      host: 你的Redis地址
      port: 6379
```

### SRS（直播流媒体服务器）

SRS 相关配置分散在多处：

| 改什么 | 文件 | 说明 |
|--------|------|------|
| SRS 回调地址 | `srs/srs.conf` | `on_publish` 和 `on_unpublish` 中的 `host.docker.internal:9001` 改为 base-service 的实际地址 |
| 推流地址 | `base-service/.../LiveRoomServiceImpl.kt` | `toDTO()` 方法中的 `rtmp://localhost:1935` |
| 拉流地址 | `base-service/.../LiveRoomServiceImpl.kt` | `toDTO()` 方法中的 `http://localhost:8080` |
| 前端 OBS 显示 | `frontend/src/components/views/RoomView.js` | 页面中硬编码的 `rtmp://localhost:1935/live` |
| Docker 端口 | `docker-compose.yml` | SRS 的 `1935`、`8080`、`1985` 端口映射 |

### Gateway 路由

如果修改了 base-service 或 mall-service 的地址/端口，需要同步修改网关路由：

文件：`gateway/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: base-service-user
          uri: http://你的base-service地址:9001   # 修改这里
          predicates:
            - Path=/api/user/**
        # ... 其他路由同理
```

### Feign 服务间调用

mall-service 通过 Feign 调用 base-service，默认地址 `http://localhost:9001`。修改方式：

在 `mall-service/src/main/resources/application.yml` 中添加：

```yaml
feign:
  base-service:
    url: http://你的base-service地址:9001
```

### 前端 API 地址

文件：`frontend/src/stores/app.js`，修改 `apiBase` 变量：

```javascript
apiBase: 'http://你的Gateway地址:9000'
```

## 端口汇总

| 服务 | 默认端口 |
|------|----------|
| Gateway | 9000 |
| base-service | 9001 |
| mall-service | 9002 |
| MySQL | 3306 |
| Redis | 6379 |
| SRS RTMP | 1935 |
| SRS HTTP-FLV | 8080 |
| SRS API | 1985 |

## 打包部署

执行 `./start.sh package`（Linux/macOS）或 `start.bat package`（Windows）后，会在 `dist/` 目录生成部署包：

- Linux/macOS: `dist/live-commerce.tar.gz`
- Windows: `dist\live-commerce.zip`

部署包结构：

```
live-commerce/
├── lib/                        # 可执行 JAR
│   ├── base-service-1.0.0-SNAPSHOT.jar
│   ├── mall-service-1.0.0-SNAPSHOT.jar
│   └── gateway-1.0.0-SNAPSHOT.jar
├── conf/                       # 配置文件（部署时在此修改）
│   ├── base-service.yml
│   ├── mall-service.yml
│   └── gateway.yml
├── sql/init.sql                # 建库 + 种子数据
├── srs/srs.conf                # SRS 流媒体配置
├── docker-compose.yml          # 中间件编排
├── frontend/                   # Vue 前端源码与入口（frontend/index.html）
├── deploy.sh                   # Linux 部署脚本
├── deploy.bat                  # Windows 部署脚本
└── logs/                       # 运行时日志目录
```

### 部署步骤

```bash
# 1. 上传到服务器并解压
tar -xzf live-commerce.tar.gz
cd live-commerce

# 2. 修改配置（MySQL/Redis 地址、端口、密码等）
vim conf/base-service.yml
vim conf/mall-service.yml
vim conf/gateway.yml

# 3. 启动中间件（或跳过，使用已有的 MySQL/Redis/SRS）
docker compose up -d

# 4. 首次部署导入种子数据
mysql -uroot -p < sql/init.sql

# 5. 启动服务
./deploy.sh start

# 6. 其他操作
./deploy.sh stop       # 停止
./deploy.sh restart    # 重启
./deploy.sh status     # 查看状态
```

Windows 服务器将上述 `./deploy.sh` 替换为 `deploy.bat` 即可。

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

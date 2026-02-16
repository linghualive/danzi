#!/bin/bash
#
# 一键启动脚本 — 全 Docker Compose 方案
#
# 用法:
#   ./run.sh              启动全部（本地构建 + 启动）
#   ./run.sh start        同上
#   ./run.sh stop         停止所有容器
#   ./run.sh restart      重启所有容器
#   ./run.sh status       查看容器状态
#   ./run.sh logs [svc]   查看日志（可指定服务名）
#   ./run.sh seed         导入种子数据
#   ./run.sh rebuild      重新编译 Java 服务并重启
#   ./run.sh down         停止并删除容器
#   ./run.sh clean        删除容器 + 数据卷
#   ./run.sh export       本地构建并打包部署包（传到服务器用）
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
PROJECT_NAME="live-commerce"
IMAGES="live-commerce/base-service:latest live-commerce/mall-service:latest live-commerce/gateway:latest"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ========== Pre-checks ==========

check_docker() {
    if ! command -v docker &>/dev/null; then
        log_error "未安装 Docker，请先安装: https://docs.docker.com/get-docker/"
        exit 1
    fi
    if ! docker info &>/dev/null; then
        log_error "Docker 未启动，请先启动 Docker"
        exit 1
    fi
    if ! docker compose version &>/dev/null; then
        log_error "需要 Docker Compose V2，请升级 Docker"
        exit 1
    fi
}

# ========== Commands ==========

do_start() {
    check_docker

    # Auto-load images if images.tar exists (server deployment)
    if [ -f "images.tar" ]; then
        log_info "检测到 images.tar，加载预构建镜像..."
        docker load < images.tar
        rm -f images.tar
        log_info "镜像加载完成"
    fi

    if [ -f "Dockerfile" ]; then
        log_info "构建并启动所有服务..."
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build
    else
        log_info "启动所有服务（使用预构建镜像）..."
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d
    fi

    echo ""
    log_info "所有服务已启动！等待健康检查完成..."
    echo ""

    wait_healthy

    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}  服务启动完成！${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "  前端页面:      http://localhost:${FRONTEND_PORT:-80}"
    echo "  Gateway:       http://localhost:${GATEWAY_PORT:-9000}"
    echo "  base-service:  http://localhost:9001"
    echo "  mall-service:  http://localhost:9002"
    echo "  SRS RTMP:      rtmp://localhost:${SRS_RTMP_PORT:-1935}/live/"
    echo "  SRS HTTP-FLV:  http://localhost:${SRS_HTTP_PORT:-8080}"
    echo ""
    echo "  提示: 运行 ./run.sh seed 导入示例数据"
    echo ""
}

do_stop() {
    check_docker
    log_info "停止所有容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" stop
    log_info "所有容器已停止"
}

do_restart() {
    check_docker
    log_info "重启所有容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" restart
    wait_healthy
    log_info "所有容器已重启"
}

do_status() {
    check_docker
    echo ""
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
    echo ""
}

do_logs() {
    check_docker
    local service="$1"
    if [ -n "$service" ]; then
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f "$service"
    else
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f
    fi
}

do_seed() {
    check_docker
    log_info "导入种子数据..."

    if ! docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps mysql | grep -q "running"; then
        log_error "MySQL 未运行，请先启动: ./run.sh start"
        exit 1
    fi

    log_info "等待 JPA 自动建表..."
    sleep 5

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T mysql \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123}" < sql/init.sql

    log_info "种子数据导入完成！"
    echo ""
    echo "  测试账号:"
    echo "    anchor1 / 123456 (美妆主播小美)"
    echo "    anchor2 / 123456 (数码达人老王)"
    echo ""
}

do_rebuild() {
    check_docker
    log_info "重新编译 Java 服务..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build \
        base-service mall-service gateway
    wait_healthy
    log_info "Java 服务重新编译并启动完成"
}

do_down() {
    check_docker
    log_info "停止并删除容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down
    log_info "容器已删除"
}

do_clean() {
    check_docker
    log_warn "将删除所有容器和数据卷（MySQL/Redis 数据将丢失）！"
    read -p "确认继续？(y/N) " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v
        log_info "容器和数据卷已删除"
    else
        log_info "已取消"
    fi
}

do_export() {
    check_docker
    log_info "开始构建并打包部署包..."

    # Step 1: Build images locally
    log_info "[1/4] 构建 Docker 镜像..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build

    # Step 2: Prepare dist directory
    log_info "[2/4] 准备部署文件..."
    local dist_dir="dist/live-commerce-docker"
    rm -rf "$dist_dir"
    mkdir -p "$dist_dir"

    # Copy needed files
    cp -r frontend "$dist_dir/"
    cp -r nginx "$dist_dir/"
    cp -r sql "$dist_dir/"
    mkdir -p "$dist_dir/srs"
    cp srs/srs-docker.conf "$dist_dir/srs/"
    cp run.sh "$dist_dir/"
    chmod +x "$dist_dir/run.sh"

    # Generate server compose file (strip build: sections)
    sed '/^    build:/,/^      target:/d' "$COMPOSE_FILE" > "$dist_dir/$COMPOSE_FILE"

    # Step 3: Save Docker images
    log_info "[3/4] 导出 Docker 镜像 (可能需要几分钟)..."
    docker save $IMAGES > "$dist_dir/images.tar"

    # Step 4: Create archive
    log_info "[4/4] 打包压缩..."
    cd dist
    tar czf live-commerce-docker.tar.gz live-commerce-docker/
    cd "$SCRIPT_DIR"

    local size
    size=$(du -sh "dist/live-commerce-docker.tar.gz" | cut -f1)

    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}  打包完成！${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "  文件: dist/live-commerce-docker.tar.gz ($size)"
    echo ""
    echo "  部署到服务器:"
    echo "    1. scp dist/live-commerce-docker.tar.gz user@server:/opt/"
    echo "    2. ssh user@server"
    echo "    3. cd /opt && tar xzf live-commerce-docker.tar.gz"
    echo "    4. cd live-commerce-docker"
    echo "    5. ./run.sh start"
    echo "    6. ./run.sh seed    # 导入示例数据"
    echo ""
}

wait_healthy() {
    local services=("lc-mysql" "lc-redis" "lc-base-service" "lc-mall-service" "lc-gateway")
    local max_wait=300
    local elapsed=0

    for svc in "${services[@]}"; do
        while [ $elapsed -lt $max_wait ]; do
            local status
            status=$(docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "not_found")

            case "$status" in
                healthy)
                    log_info "$svc: 就绪"
                    break
                    ;;
                unhealthy)
                    log_warn "$svc: 不健康，查看日志: ./run.sh logs"
                    break
                    ;;
                not_found)
                    break
                    ;;
                *)
                    sleep 5
                    elapsed=$((elapsed + 5))
                    ;;
            esac
        done
    done

    if [ $elapsed -ge $max_wait ]; then
        log_warn "部分服务启动超时，使用 ./run.sh status 查看状态"
    fi
}

# ========== Main ==========

case "${1:-start}" in
    start)    do_start ;;
    stop)     do_stop ;;
    restart)  do_restart ;;
    status)   do_status ;;
    logs)     do_logs "$2" ;;
    seed)     do_seed ;;
    rebuild)  do_rebuild ;;
    down)     do_down ;;
    clean)    do_clean ;;
    export)   do_export ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs|seed|rebuild|down|clean|export}"
        echo ""
        echo "命令说明:"
        echo "  start     构建并启动全部服务（服务器上自动加载预构建镜像）"
        echo "  stop      停止所有容器"
        echo "  restart   重启所有容器"
        echo "  status    查看容器状态"
        echo "  logs      查看日志 (可加服务名: ./run.sh logs base-service)"
        echo "  seed      导入种子数据"
        echo "  rebuild   重新编译 Java 服务并重启"
        echo "  down      停止并删除容器"
        echo "  clean     删除容器 + 数据卷"
        echo "  export    本地构建并打包部署包"
        exit 1
        ;;
esac

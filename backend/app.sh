#!/bin/bash
# ============================================
# 应用启停脚本
# 用法: ./app.sh {start|stop|restart|status}
# ============================================

APP_NAME="base"
JAR_FILE=$(ls -t target/*.jar 2>/dev/null | head -1)
LOG_DIR="./logs"
PID_FILE="./app.pid"
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseZGC"
SPRING_PROFILES="prod"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 检查jar包
check_jar() {
    if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}[ERROR] 未找到 jar 包，请先执行 mvn package${NC}"
        exit 1
    fi
}

# 获取PID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return
        fi
    fi
    # PID文件不存在或进程已死，尝试通过进程名查找
    pgrep -f "$JAR_FILE" 2>/dev/null | head -1
}

# 启动
start() {
    check_jar
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}[WARN] $APP_NAME 已在运行 (PID: $pid)${NC}"
        return
    fi

    mkdir -p "$LOG_DIR"
    echo -e "${GREEN}[INFO] 启动 $APP_NAME ...${NC}"
    echo -e "${GREEN}[INFO] JAR: $JAR_FILE${NC}"
    echo -e "${GREEN}[INFO] Profile: $SPRING_PROFILES${NC}"

    nohup java $JAVA_OPTS \
        -jar "$JAR_FILE" \
        --spring.profiles.active="$SPRING_PROFILES" \
        > /dev/null 2>&1 &

    echo $! > "$PID_FILE"
    sleep 2

    pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo -e "${GREEN}[INFO] 启动成功 (PID: $pid)${NC}"
    else
        echo -e "${RED}[ERROR] 启动失败，请检查日志: $LOG_DIR/${APP_NAME}.log${NC}"
        rm -f "$PID_FILE"
        exit 1
    fi
}

# 停止
stop() {
    local pid=$(get_pid)
    if [ -z "$pid" ]; then
        echo -e "${YELLOW}[WARN] $APP_NAME 未在运行${NC}"
        rm -f "$PID_FILE"
        return
    fi

    echo -e "${GREEN}[INFO] 停止 $APP_NAME (PID: $pid) ...${NC}"
    kill "$pid"

    # 等待进程退出（最多30秒）
    local count=0
    while kill -0 "$pid" 2>/dev/null; do
        sleep 1
        count=$((count + 1))
        if [ $count -ge 30 ]; then
            echo -e "${YELLOW}[WARN] 优雅停止超时，强制终止${NC}"
            kill -9 "$pid"
            break
        fi
    done

    rm -f "$PID_FILE"
    echo -e "${GREEN}[INFO] 已停止${NC}"
}

# 重启
restart() {
    stop
    sleep 1
    start
}

# 状态
status() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        echo -e "${GREEN}[INFO] $APP_NAME 正在运行 (PID: $pid)${NC}"
        # 显示资源占用
        ps -p "$pid" -o pid,user,%cpu,%mem,etime,command --no-headers 2>/dev/null || \
        ps -p "$pid" -o pid,user,%cpu,%mem,etime 2>/dev/null
    else
        echo -e "${YELLOW}[INFO] $APP_NAME 未运行${NC}"
    fi
}

# 主入口
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

#!/bin/bash
# ============================================
# 基于脚手架初始化新项目
# 用法: ./init-project.sh <项目名> <包名> <目标路径>
# 示例: ./init-project.sh my-app com.zc.myapp ~/IdeaProjects/my-app
# ============================================

set -e

# 脚手架根目录
SCAFFOLD_DIR="$(cd "$(dirname "$0")" && pwd)"

# 参数校验
if [ $# -lt 3 ]; then
    echo "用法: $0 <项目名> <包名> <目标路径>"
    echo "示例: $0 my-app com.zc.myapp ~/IdeaProjects/my-app"
    echo ""
    echo "参数说明:"
    echo "  项目名  - Maven artifactId，也用于前端 package name"
    echo "  包名    - Java 包名，如 com.zc.myapp"
    echo "  目标路径 - 新项目存放位置"
    exit 1
fi

PROJECT_NAME="$1"
PACKAGE_NAME="$2"
TARGET_DIR="$3"

# 源包名和路径
SRC_PACKAGE="com.zc.base"
SRC_PACKAGE_RE="com\.zc\.base"
SRC_PACKAGE_PATH="com/zc/base"
SRC_APP_CLASS="BaseApplication"

# 目标包路径
TARGET_PACKAGE_PATH="$(echo "$PACKAGE_NAME" | tr . /)"

# 数据库名：项目名里的 - 换成 _，开发库加 _dev 后缀
DB_NAME="${PROJECT_NAME//-/_}"

# 原地替换。macOS(BSD sed) 和 Linux(GNU sed) 都支持 -i 紧跟后缀的写法，
# 替换完成后统一删除 .bak 备份
sed_i() {
    sed -i.bak "$@"
}

# 目标 Application 类名（取包名最后一段首字母大写 + Application）
LAST_SEGMENT="${PACKAGE_NAME##*.}"
APP_CLASS="$(echo "${LAST_SEGMENT:0:1}" | tr '[:lower:]' '[:upper:]')${LAST_SEGMENT:1}Application"

echo "========================================"
echo "  初始化新项目"
echo "========================================"
echo "  项目名: $PROJECT_NAME"
echo "  包名:   $PACKAGE_NAME"
echo "  类名:   $APP_CLASS"
echo "  目标:   $TARGET_DIR"
echo "========================================"
echo ""

# 检查目标目录
if [ -d "$TARGET_DIR" ]; then
    echo "错误: 目标目录已存在: $TARGET_DIR"
    exit 1
fi

# 复制脚手架（排除.git、node_modules、target等）
echo "[1/5] 复制脚手架文件..."
mkdir -p "$TARGET_DIR"
rsync -a --exclude='.git' --exclude='node_modules' --exclude='target' \
    --exclude='.idea' --exclude='dist' --exclude='logs' --exclude='*.pid' \
    --exclude='.DS_Store' --exclude='init-project.sh' \
    "$SCAFFOLD_DIR/" "$TARGET_DIR/"

# 重命名 Java 包目录
echo "[2/5] 重命名包目录..."
BACKEND_SRC="$TARGET_DIR/backend/src/main/java"
BACKEND_TEST="$TARGET_DIR/backend/src/test/java"

mkdir -p "$BACKEND_SRC/$TARGET_PACKAGE_PATH"
if [ -d "$BACKEND_SRC/$SRC_PACKAGE_PATH" ]; then
    mv "$BACKEND_SRC/$SRC_PACKAGE_PATH/"* "$BACKEND_SRC/$TARGET_PACKAGE_PATH/" 2>/dev/null || true
    # 清理空的源包目录
    rm -rf "$BACKEND_SRC/com/zc/base"
    # 如果 com/zc 空了也删
    rmdir "$BACKEND_SRC/com/zc" 2>/dev/null || true
    rmdir "$BACKEND_SRC/com" 2>/dev/null || true
fi

if [ -d "$BACKEND_TEST/$SRC_PACKAGE_PATH" ]; then
    mkdir -p "$BACKEND_TEST/$TARGET_PACKAGE_PATH"
    mv "$BACKEND_TEST/$SRC_PACKAGE_PATH/"* "$BACKEND_TEST/$TARGET_PACKAGE_PATH/" 2>/dev/null || true
    rm -rf "$BACKEND_TEST/com/zc/base"
    rmdir "$BACKEND_TEST/com/zc" 2>/dev/null || true
    rmdir "$BACKEND_TEST/com" 2>/dev/null || true
fi

# 替换包名和类名
echo "[3/5] 替换包名和类名..."
find "$TARGET_DIR/backend" -type f \( -name "*.java" -o -name "*.xml" -o -name "*.yml" -o -name "*.properties" \) \
    -exec sed -i.bak "s|$SRC_PACKAGE_RE|$PACKAGE_NAME|g" {} +

find "$TARGET_DIR/backend" -type f -name "*.java" \
    -exec sed -i.bak "s|$SRC_APP_CLASS|$APP_CLASS|g" {} +

# 重命名 Application 文件
if [ -f "$BACKEND_SRC/$TARGET_PACKAGE_PATH/BaseApplication.java" ]; then
    mv "$BACKEND_SRC/$TARGET_PACKAGE_PATH/BaseApplication.java" \
       "$BACKEND_SRC/$TARGET_PACKAGE_PATH/${APP_CLASS}.java"
fi

# 替换 Maven artifactId 和前端 package name
echo "[4/5] 替换项目名..."
sed_i "s|<artifactId>base</artifactId>|<artifactId>$PROJECT_NAME</artifactId>|g" "$TARGET_DIR/backend/pom.xml"
sed_i "s|<name>base</name>|<name>$PROJECT_NAME</name>|g" "$TARGET_DIR/backend/pom.xml"
sed_i "s|\"name\": \"base-admin\"|\"name\": \"$PROJECT_NAME-admin\"|g" "$TARGET_DIR/fronted-admin/package.json"

# application.yml 中的配置前缀
find "$TARGET_DIR/backend/src/main/resources" -type f -name "*.yml" \
    -exec sed -i.bak "s|^base:|$LAST_SEGMENT:|g" {} +
find "$TARGET_DIR/backend" -type f -name "*.java" \
    -exec sed -i.bak "s|prefix = \"base\.|prefix = \"$LAST_SEGMENT.|g" {} +

# 应用名、数据库名、启停脚本和 docker-compose 里的名字
RES_DIR="$TARGET_DIR/backend/src/main/resources"
sed_i "s|^    name: base$|    name: $PROJECT_NAME|" "$RES_DIR/application.yml"
sed_i "s|/base_dev?|/${DB_NAME}_dev?|" "$RES_DIR/application-dev.yml"
sed_i "s|DB_NAME:base}|DB_NAME:${DB_NAME}}|" "$RES_DIR/application-prod.yml"
sed_i "s|defaultValue=\"base\"|defaultValue=\"$PROJECT_NAME\"|" "$RES_DIR/logback-spring.xml"
sed_i "s|^APP_NAME=\"base\"|APP_NAME=\"$PROJECT_NAME\"|" "$TARGET_DIR/backend/app.sh"
sed_i "s|base_dev|${DB_NAME}_dev|g; s|base-mysql|$PROJECT_NAME-mysql|; s|base-redis|$PROJECT_NAME-redis|" "$TARGET_DIR/docker-compose.yml"
sed_i "s|base_dev|${DB_NAME}_dev|g; s|$SRC_PACKAGE_RE|$PACKAGE_NAME|g" "$TARGET_DIR/README.md"

# 清理 sed 的备份文件
find "$TARGET_DIR" -type f -name "*.bak" -delete

# 初始化 git
echo "[5/5] 初始化 Git 仓库..."
cd "$TARGET_DIR" && git init && git add -A && git commit -m "init: scaffold from base framework" --quiet

echo ""
echo "========================================"
echo "  项目初始化完成!"
echo "========================================"
echo ""
echo "依赖服务 (MySQL + Redis):"
echo "  cd $TARGET_DIR && docker compose up -d"
echo ""
echo "后端启动 (JDK 21，首次启动由 Flyway 自动建表):"
echo "  cd $TARGET_DIR/backend"
echo "  mvn spring-boot:run"
echo ""
echo "前端启动:"
echo "  cd $TARGET_DIR/fronted-admin"
echo "  npm install && npm run dev"
echo ""
echo "接口文档: http://localhost:8080/swagger-ui.html"
echo "默认账号: admin / admin123"
echo ""

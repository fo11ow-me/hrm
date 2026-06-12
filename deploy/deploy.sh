#!/bin/bash
# 本地一键构建部署脚本
# 用法: bash deploy.sh [--test]
set -euo pipefail

LAST_ERROR=""
handle_exit() {
  local code=$?
  if [ $code -eq 0 ]; then
    echo "success"
  else
    echo "fail (exit code: $code)"
    if [ -n "$LAST_ERROR" ]; then
      echo ""
      echo "========== 错误详情 =========="
      echo "$LAST_ERROR" | tail -30
    fi
  fi
}
trap handle_exit EXIT

SKIP_TESTS=true
if [ "${1:-}" = "--test" ]; then
  SKIP_TESTS=false
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 加载 .env
if [ -f ../.env ]; then
  set -a; source ../.env; set +a
fi

REQUIRED_VARS="SERVER_HOST SERVER_USER REGISTRY_USER REGISTRY_PASSWORD"
for v in $REQUIRED_VARS; do
  if [ -z "${!v:-}" ]; then
    echo "错误: .env 缺少变量 $v"
    exit 1
  fi
done

SHA="$(git rev-parse --short HEAD)"
REGISTRY="${SERVER_HOST}:5000"
IMAGE_SERVER="${REGISTRY}/hrm/hrm-server"
IMAGE_NGINX="${REGISTRY}/hrm/hrm-nginx"

# 检测哪些模块需要重新构建
LAST_DEPLOYED_FILE="$SCRIPT_DIR/.last-deployed"
LAST_SHA=""
if [ -f "$LAST_DEPLOYED_FILE" ]; then
  LAST_SHA="$(cat "$LAST_DEPLOYED_FILE")"
fi

REBUILD_SERVER=true
REBUILD_ADMIN=true
if [ -n "$LAST_SHA" ] && git rev-parse --verify "$LAST_SHA" > /dev/null 2>&1; then
  if ! git diff --name-only "$LAST_SHA"..HEAD | grep -qE 'hrm-server/(src/|pom\.xml|Dockerfile)'; then
    REBUILD_SERVER=false
  fi
  if ! git diff --name-only "$LAST_SHA"..HEAD | grep -qE 'hrm-admin/(src/|public/|package.*\.json|vue\.config)'; then
    REBUILD_ADMIN=false
  fi
fi

echo "========== 1. 检查前置条件 =========="

if ! docker info > /dev/null 2>&1; then
  echo "错误: Docker 未运行"
  exit 1
fi

if [ -n "$(git status --porcelain | grep '^?')" ]; then
  echo "错误: 工作区有未跟踪的新文件，请先 commit"
  exit 1
fi

if ! ssh -o ConnectTimeout=5 -o BatchMode=yes "${SERVER_USER}@${SERVER_HOST}" echo ok > /dev/null 2>&1; then
  echo "错误: 无法 SSH 连接到 ${SERVER_USER}@${SERVER_HOST}"
  exit 1
fi

if [ -n "$LAST_SHA" ]; then
  echo "Docker: ok  SSH: ok  Git: clean  上次部署: ${LAST_SHA}"
else
  echo "Docker: ok  SSH: ok  Git: clean  首次部署，全量构建"
fi

echo ""
echo "========== 2. 运行后端测试 =========="
if $SKIP_TESTS; then
  echo "跳过 (默认); 使用 --test 开启"
else
  cd "$SCRIPT_DIR"/../hrm-server
  mvn test -DskipITs=true -q
  cd "$SCRIPT_DIR"
  echo "Tests: passed"
fi

echo ""
echo "========== 3. 构建阶段 =========="
echo "后端: $($REBUILD_SERVER && echo '需构建(Docker内Maven)' || echo '跳过')  前端: $($REBUILD_ADMIN && echo '需构建' || echo '跳过')"

# 前端本地构建（后端 Maven 编译已移入 Dockerfile，避免 Windows 文件锁定）
ADMIN_PID=""

if $REBUILD_ADMIN; then
  ADMIN_LOG=$(mktemp)
  (cd "$SCRIPT_DIR"/../hrm-admin && npm run build 2>"$ADMIN_LOG" | tail -1) &
  ADMIN_PID=$!
  wait "$ADMIN_PID" || { LAST_ERROR=$(cat "$ADMIN_LOG"); rm -f "$ADMIN_LOG"; echo "前端构建失败"; exit 1; }
  rm -f "$ADMIN_LOG"
fi

echo "构建完成"

echo ""
echo "========== 4. 构建 Docker 镜像 (latest + ${SHA}) =========="
echo "${REGISTRY_PASSWORD}" | docker login "${REGISTRY}" -u "${REGISTRY_USER}" --password-stdin > /dev/null 2>&1

PUSH_PIDS=""

if $REBUILD_SERVER; then
  DOCKER_LOG=$(mktemp)
  docker build --platform linux/amd64 \
    -t "${IMAGE_SERVER}:latest" \
    -t "${IMAGE_SERVER}:${SHA}" \
    -f ../hrm-server/Dockerfile ../hrm-server/ 2>"$DOCKER_LOG" || { LAST_ERROR=$(cat "$DOCKER_LOG"); rm -f "$DOCKER_LOG"; echo "Docker构建hrm-server失败"; exit 1; }
  rm -f "$DOCKER_LOG"
  echo "  ${IMAGE_SERVER}:latest"
  echo "  ${IMAGE_SERVER}:${SHA}"
else
  docker tag "${IMAGE_SERVER}:latest" "${IMAGE_SERVER}:${SHA}" 2>/dev/null || true
  echo "  ${IMAGE_SERVER}:${SHA} (复用)"
fi

if $REBUILD_ADMIN; then
  DOCKER_LOG=$(mktemp)
  docker build --platform linux/amd64 \
    -t "${IMAGE_NGINX}:latest" \
    -t "${IMAGE_NGINX}:${SHA}" \
    -f nginx/Dockerfile .. 2>"$DOCKER_LOG" || { LAST_ERROR=$(cat "$DOCKER_LOG"); rm -f "$DOCKER_LOG"; echo "Docker构建hrm-nginx失败"; exit 1; }
  rm -f "$DOCKER_LOG"
  echo "  ${IMAGE_NGINX}:latest"
  echo "  ${IMAGE_NGINX}:${SHA}"
else
  docker tag "${IMAGE_NGINX}:latest" "${IMAGE_NGINX}:${SHA}" 2>/dev/null || true
  echo "  ${IMAGE_NGINX}:${SHA} (复用)"
fi

echo ""
echo "========== 5. 推送镜像到 Registry（并行） =========="

if $REBUILD_SERVER; then
  PUSH_LOG=$(mktemp)
  (docker push "${IMAGE_SERVER}:latest" && docker push "${IMAGE_SERVER}:${SHA}") 2>"$PUSH_LOG" &
  PUSH_PIDS="$!"
fi

if $REBUILD_ADMIN; then
  (docker push "${IMAGE_NGINX}:latest" && docker push "${IMAGE_NGINX}:${SHA}") 2>/dev/null &
  PUSH_PIDS="$PUSH_PIDS $!"
fi

if [ -n "$PUSH_PIDS" ]; then
  for pid in $PUSH_PIDS; do wait "$pid" || { LAST_ERROR=$(cat "$PUSH_LOG" 2>/dev/null); rm -f "$PUSH_LOG"; echo "镜像推送失败"; exit 1; }; done
fi
rm -f "$PUSH_LOG"
echo "推送完成"

echo ""
echo "清理本地旧标签..."
ALL_TAGS=$(docker image ls --format '{{.Repository}}:{{.Tag}}' \
  | grep "^${REGISTRY}/hrm/" \
  | grep -v ':latest' \
  | sort)
KEEP_COUNT=3
COUNT=0
echo "$ALL_TAGS" | while read -r tag; do
  COUNT=$((COUNT + 1))
  if [ $COUNT -gt $KEEP_COUNT ]; then
    docker rmi "$tag" 2>/dev/null || true
    echo "  删除: $tag"
  fi
done

# 记录本次部署 SHA
echo "$SHA" > "$LAST_DEPLOYED_FILE"

echo ""
echo "========== 6. SSH 触发服务器部署 =========="
SSH_LOG=$(mktemp)
ssh -o StrictHostKeyChecking=no \
  "${SERVER_USER}@${SERVER_HOST}" \
  "bash /opt/app/hrm/deploy-server.sh" 2>"$SSH_LOG" || { LAST_ERROR=$(cat "$SSH_LOG"); rm -f "$SSH_LOG"; echo "服务器部署失败"; exit 1; }
rm -f "$SSH_LOG"

echo ""
echo "========== 完成 =========="
echo "当前版本: ${SHA}"

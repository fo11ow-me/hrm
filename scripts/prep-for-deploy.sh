#!/bin/bash
# 部署前代码准备脚本
# 运行方式: bash scripts/prep-for-deploy.sh

set -e

echo "=== HRM 部署前代码准备 ==="
echo ""

# 1. 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
echo "当前分支: $CURRENT_BRANCH"

if [ "$CURRENT_BRANCH" != "dev" ]; then
    echo ""
    echo "警告: 当前不在 dev 分支"
    echo "建议执行: git checkout dev"
    echo ""
fi

# 2. 检查远程仓库
echo "远程仓库:"
git remote -v
echo ""

# 3. 显示变更统计
echo "变更统计:"
git status --short | wc -l
echo "文件有变更"
echo ""

# 4. 检查是否有敏感文件
echo "检查敏感文件..."
SENSITIVE_FILES=$(git ls-files | grep -E "application-prod.yml|application-dev.yml|.env$" | grep -v ".env.example" || true)
if [ -n "$SENSITIVE_FILES" ]; then
    echo "警告: 发现敏感文件在仓库中:"
    echo "$SENSITIVE_FILES"
    echo "请先删除这些文件并提交"
    exit 1
else
    echo "✓ 未发现敏感文件在仓库中"
fi
echo ""

# 5. 检查是否有待提交的变更
UNCOMMITTED=$(git status --short)
if [ -n "$UNCOMMITTED" ]; then
    echo "待提交的变更:"
    git status --short | head -20
    echo ""
    echo "... 还有更多变更"
    echo ""
    echo "建议执行:"
    echo "  git add ."
    echo "  git commit -m 'feat: migrate to hrm-server and hrm-admin structure'"
    echo ""
else
    echo "✓ 所有变更已提交"
fi

echo "=== 准备完成 ==="
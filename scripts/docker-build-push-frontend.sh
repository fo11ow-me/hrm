#!/bin/bash

# 配置区
IMAGE_NAME="qiu/hrm-vue"
IMAGE_TAG="latest"

# 你的私有仓库地址（改成你的服务器IP）
REGISTRY="47.106.93.24:5000"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo "构建 Vue 镜像..."
docker build --no-cache -t ${FULL_IMAGE_NAME} .
if [ $? -ne 0 ]; then
  echo "Docker build 失败"
  exit 1
fi

echo "打标签..."
docker tag ${FULL_IMAGE_NAME} ${REGISTRY}/${FULL_IMAGE_NAME}

echo "推送到仓库..."
docker push ${REGISTRY}/${FULL_IMAGE_NAME}
if [ $? -ne 0 ]; then
  echo "docker push 失败"
  exit 1
fi

echo "推送成功：${FULL_IMAGE_NAME}"
echo "完成！"

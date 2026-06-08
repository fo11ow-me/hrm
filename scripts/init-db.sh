#!/bin/bash

########################################
# Docker 容器
########################################
CONTAINER_NAME="mysql8"

DB_USER="root"
DB_PASS="123456"

########################################
# SQL 文件与对应数据库
########################################
declare -A DB_MAP

DB_MAP["/opt/sql/hrm.sql"]="hrm"
DB_MAP["/opt/sql/hrm_activiti.sql"]="hrm_activiti"

########################################
# 日志与锁
########################################
LOG_FILE="/opt/scripts/init_db.log"
LOCK_FILE="/tmp/init_db.lock"

source /etc/profile

echo "========== $(date '+%Y-%m-%d %H:%M:%S') ==========" >> $LOG_FILE

########################################
# 防重复执行
########################################
if [ -f "$LOCK_FILE" ]; then
    echo "[WARN] 任务正在执行，已跳过" >> $LOG_FILE
    exit 1
fi

touch $LOCK_FILE

########################################
# 检查容器
########################################
docker ps | grep $CONTAINER_NAME > /dev/null
if [ $? -ne 0 ]; then
    echo "[ERROR] MySQL 容器未运行: $CONTAINER_NAME" >> $LOG_FILE
    rm -f $LOCK_FILE
    exit 1
fi

########################################
# 执行多个数据库初始化
########################################
for SQL_FILE in "${!DB_MAP[@]}"
do
    DB_NAME=${DB_MAP[$SQL_FILE]}

    echo "[INFO] 开始初始化数据库: $DB_NAME" >> $LOG_FILE

    if [ ! -f "$SQL_FILE" ]; then
        echo "[ERROR] SQL 文件不存在: $SQL_FILE" >> $LOG_FILE
        continue
    fi

    docker exec -i $CONTAINER_NAME \
        mysql -u$DB_USER -p$DB_PASS $DB_NAME \
        < $SQL_FILE >> $LOG_FILE 2>&1

    if [ $? -eq 0 ]; then
        echo "[SUCCESS] 初始化成功: $DB_NAME <- $SQL_FILE" >> $LOG_FILE
    else
        echo "[ERROR] 初始化失败: $DB_NAME <- $SQL_FILE" >> $LOG_FILE
        rm -f $LOCK_FILE
        exit 1
    fi
done

########################################
# 清理
########################################
rm -f $LOCK_FILE

echo "[INFO] 所有数据库初始化完成" >> $LOG_FILE
echo "" >> $LOG_FILE

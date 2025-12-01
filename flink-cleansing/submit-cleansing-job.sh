#!/bin/bash

# Flink清洗作业提交脚本

FLINK_HOME="/opt/app/flink"
JAR_FILE="flink-cleansing-1.0.jar"
MAIN_CLASS="com.flink.realtime.cleansing.FlinkCleansingJob"
JOB_NAME="Flink-Cleansing-Job"

echo "正在提交Flink清洗作业..."

if [ ! -f "$JAR_FILE" ]; then
    echo "错误: JAR文件不存在，请先运行 mvn clean package"
    exit 1
fi

# 提交到Flink集群
$FLINK_HOME/bin/flink run \
    -c $MAIN_CLASS \
    -d \
    $JAR_FILE

echo "Flink清洗作业已提交"
echo "查看作业状态: http://master1:8081"

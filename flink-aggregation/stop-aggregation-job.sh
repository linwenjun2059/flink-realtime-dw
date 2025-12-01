#!/bin/bash

# Flink聚合作业停止脚本

FLINK_HOME="/opt/app/flink"
JOB_NAME="Flink Real-time Aggregation Job"

echo "正在停止Flink聚合作业..."

# 查找作业ID
JOB_ID=$($FLINK_HOME/bin/flink list -r 2>/dev/null | grep -i "aggregation" | awk '{print $4}')

if [ -z "$JOB_ID" ]; then
    echo "未找到运行中的聚合作业"
    echo "查看所有运行中的作业："
    $FLINK_HOME/bin/flink list -r
    exit 1
fi

echo "找到作业ID: $JOB_ID"
echo "正在取消作业..."

# 取消作业
$FLINK_HOME/bin/flink cancel $JOB_ID

if [ $? -eq 0 ]; then
    echo "✅ Flink聚合作业已成功停止"
else
    echo "❌ 停止作业失败，请检查日志"
    exit 1
fi

echo "查看剩余运行中的作业："
$FLINK_HOME/bin/flink list -r

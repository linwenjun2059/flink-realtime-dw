#!/bin/bash

# 数据生成器启动脚本

JAR_FILE="data-generator-1.0.jar"
MAIN_CLASS="com.flink.realtime.generator.DataGenerator"

echo "正在启动数据生成器..."

if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到JAR文件。请先运行 'mvn clean package'。"
    exit 1
fi

java -cp $JAR_FILE $MAIN_CLASS

echo "数据生成器已完成。"

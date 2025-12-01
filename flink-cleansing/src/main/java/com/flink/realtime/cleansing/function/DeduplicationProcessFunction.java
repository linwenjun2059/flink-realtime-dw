package com.flink.realtime.cleansing.function;

import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 数据去重函数（使用KeyedState + TTL）
 */
public class DeduplicationProcessFunction<T> extends KeyedProcessFunction<String, T, T> {
    
    private ValueState<Boolean> seenState;
    
    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<Boolean> descriptor = 
            new ValueStateDescriptor<>("seen-state", Boolean.class);
        
        // 设置TTL为1小时，超过1小时后状态自动清除
        StateTtlConfig ttlConfig = StateTtlConfig
            .newBuilder(Time.hours(1))
            .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
            .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
            .build();
        descriptor.enableTimeToLive(ttlConfig);
        
        seenState = getRuntimeContext().getState(descriptor);
    }
    
    @Override
    public void processElement(T value, Context ctx, Collector<T> out) throws Exception {
        // 检查是否已经见过这条数据
        if (seenState.value() == null) {
            // 第一次出现，标记为已见并输出
            seenState.update(true);
            out.collect(value);
        }
        // 如果已经见过，则丢弃（不输出）
    }
}

package com.flink.realtime.aggregation.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 时间窗口聚合指标
 */
public class TimeWindowMetrics implements Serializable {
    private String windowType;       // 窗口类型: 1min/5min/1hour
    private Timestamp windowStart;    // 窗口开始时间
    private Timestamp windowEnd;      // 窗口结束时间
    private Long orderCount;         // 订单数量
    private Double totalAmount;      // 交易总额
    private Double avgAmount;        // 订单平均金额
    private Double maxAmount;        // 单笔最大金额

    public TimeWindowMetrics() {
    }

    public TimeWindowMetrics(String windowType, Timestamp windowStart, Timestamp windowEnd, 
                            Long orderCount, Double totalAmount, Double avgAmount, Double maxAmount) {
        this.windowType = windowType;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.avgAmount = avgAmount;
        this.maxAmount = maxAmount;
    }

    // Getters and Setters
    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public Timestamp getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Timestamp windowStart) {
        this.windowStart = windowStart;
    }

    public Timestamp getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Timestamp windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getAvgAmount() {
        return avgAmount;
    }

    public void setAvgAmount(Double avgAmount) {
        this.avgAmount = avgAmount;
    }

    public Double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(Double maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public String toString() {
        return "TimeWindowMetrics{" +
                "windowType='" + windowType + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", orderCount=" + orderCount +
                ", totalAmount=" + totalAmount +
                ", avgAmount=" + avgAmount +
                ", maxAmount=" + maxAmount +
                '}';
    }
}

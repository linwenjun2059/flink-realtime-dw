package com.flink.realtime.aggregation.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 城市订单分布指标
 */
public class CityMetrics implements Serializable {
    private Timestamp windowStart;
    private Timestamp windowEnd;
    private String city;             // 城市名称
    private Long orderCount;         // 订单数量
    private Double totalAmount;      // 交易总额

    public CityMetrics() {
    }

    public CityMetrics(Timestamp windowStart, Timestamp windowEnd, String city, 
                      Long orderCount, Double totalAmount) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.city = city;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
}

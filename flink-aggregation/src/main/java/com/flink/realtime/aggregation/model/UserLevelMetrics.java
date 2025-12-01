package com.flink.realtime.aggregation.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 用户等级消费指标
 */
public class UserLevelMetrics implements Serializable {
    private Timestamp windowStart;
    private Timestamp windowEnd;
    private Integer userLevel;           // 用户等级 1-5
    private Long userCount;              // 下单用户数
    private Long orderCount;             // 订单数量
    private Double totalAmount;          // 消费总额
    private Double avgAmountPerUser;     // 人均消费金额

    public UserLevelMetrics() {
    }

    public UserLevelMetrics(Timestamp windowStart, Timestamp windowEnd, Integer userLevel,
                           Long userCount, Long orderCount, Double totalAmount, Double avgAmountPerUser) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.userLevel = userLevel;
        this.userCount = userCount;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.avgAmountPerUser = avgAmountPerUser;
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

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
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

    public Double getAvgAmountPerUser() {
        return avgAmountPerUser;
    }

    public void setAvgAmountPerUser(Double avgAmountPerUser) {
        this.avgAmountPerUser = avgAmountPerUser;
    }
}

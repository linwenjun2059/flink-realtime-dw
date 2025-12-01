package com.flink.realtime.generator.model;

import java.io.Serializable;

/**
 * 订单主表实体类
 */
public class OrderMain implements Serializable {
    private String orderId;
    private String userId;
    private Integer orderStatus;
    private Double totalAmount;
    private String createTime;
    private String payTime;

    public OrderMain() {
    }

    public OrderMain(String orderId, String userId, Integer orderStatus, Double totalAmount, String createTime, String payTime) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.createTime = createTime;
        this.payTime = payTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    @Override
    public String toString() {
        return "OrderMain{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", orderStatus=" + orderStatus +
                ", totalAmount=" + totalAmount +
                ", createTime='" + createTime + '\'' +
                ", payTime='" + payTime + '\'' +
                '}';
    }
}

package com.flink.realtime.cleansing.model;

import java.io.Serializable;

/**
 * 订单主表和明细表Join后的结果
 */
public class OrderJoinResult implements Serializable {
    private String orderId;
    private String userId;
    private Integer orderStatus;
    private String createTime;
    
    private String detailId;
    private String productId;
    private Integer productNum;
    private Double productPrice;
    private Double detailAmount;

    public OrderJoinResult() {
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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getDetailId() {
        return detailId;
    }

    public void setDetailId(String detailId) {
        this.detailId = detailId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getProductNum() {
        return productNum;
    }

    public void setProductNum(Integer productNum) {
        this.productNum = productNum;
    }

    public Double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(Double productPrice) {
        this.productPrice = productPrice;
    }

    public Double getDetailAmount() {
        return detailAmount;
    }

    public void setDetailAmount(Double detailAmount) {
        this.detailAmount = detailAmount;
    }

    @Override
    public String toString() {
        return "OrderJoinResult{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", orderStatus=" + orderStatus +
                ", createTime='" + createTime + '\'' +
                ", detailId='" + detailId + '\'' +
                ", productId='" + productId + '\'' +
                ", productNum=" + productNum +
                ", productPrice=" + productPrice +
                ", detailAmount=" + detailAmount +
                '}';
    }
}

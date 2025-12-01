package com.flink.realtime.generator.model;

import java.io.Serializable;

/**
 * 订单明细表实体类
 */
public class OrderDetail implements Serializable {
    private String detailId;
    private String orderId;
    private String productId;
    private Integer productNum;
    private Double productPrice;
    private Double detailAmount;

    public OrderDetail() {
    }

    public OrderDetail(String detailId, String orderId, String productId, Integer productNum, Double productPrice, Double detailAmount) {
        this.detailId = detailId;
        this.orderId = orderId;
        this.productId = productId;
        this.productNum = productNum;
        this.productPrice = productPrice;
        this.detailAmount = detailAmount;
    }

    public String getDetailId() {
        return detailId;
    }

    public void setDetailId(String detailId) {
        this.detailId = detailId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
        return "OrderDetail{" +
                "detailId='" + detailId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", productId='" + productId + '\'' +
                ", productNum=" + productNum +
                ", productPrice=" + productPrice +
                ", detailAmount=" + detailAmount +
                '}';
    }
}

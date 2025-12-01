package com.flink.realtime.cleansing.model;

import java.io.Serializable;

/**
 * 订单宽表实体（关联所有维度后的最终结果）
 */
public class OrderWide implements Serializable {
    // 来自订单主表
    private String orderId;
    private String userId;
    private Integer orderStatus;
    private String createTime;
    
    // 来自订单明细表
    private String detailId;
    private String productId;
    private Integer productNum;
    private Double productPrice;
    private Double detailAmount;
    
    // 来自用户维度表
    private String userName;
    private Integer userLevel;
    private String userCity;
    
    // 来自商品维度表
    private String productName;
    private String category;
    private String brand;

    public OrderWide() {
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public String getUserCity() {
        return userCity;
    }

    public void setUserCity(String userCity) {
        this.userCity = userCity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public String toString() {
        return "OrderWide{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", orderStatus=" + orderStatus +
                ", createTime='" + createTime + '\'' +
                ", detailId='" + detailId + '\'' +
                ", productId='" + productId + '\'' +
                ", productNum=" + productNum +
                ", productPrice=" + productPrice +
                ", detailAmount=" + detailAmount +
                ", userName='" + userName + '\'' +
                ", userLevel=" + userLevel +
                ", userCity='" + userCity + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                '}';
    }
}

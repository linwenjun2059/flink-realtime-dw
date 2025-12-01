package com.flink.realtime.aggregation.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 商品分类Top指标
 */
public class CategoryTopMetrics implements Serializable {
    private Timestamp windowStart;
    private Timestamp windowEnd;
    private Integer rank;            // 排名 1-10
    private String category;         // 商品分类
    private Long salesCount;         // 销售数量
    private Double salesAmount;      // 销售金额
    private Long orderCount;         // 订单数量

    public CategoryTopMetrics() {
    }

    public CategoryTopMetrics(Timestamp windowStart, Timestamp windowEnd, Integer rank, 
                             String category, Long salesCount, Double salesAmount, Long orderCount) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.rank = rank;
        this.category = category;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.orderCount = orderCount;
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

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Long salesCount) {
        this.salesCount = salesCount;
    }

    public Double getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Double salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}

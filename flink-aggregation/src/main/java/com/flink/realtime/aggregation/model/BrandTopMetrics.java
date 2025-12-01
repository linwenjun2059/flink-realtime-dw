package com.flink.realtime.aggregation.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 品牌Top指标
 */
public class BrandTopMetrics implements Serializable {
    private Timestamp windowStart;
    private Timestamp windowEnd;
    private Integer rank;            // 排名 1-20
    private String brand;            // 品牌名称
    private Long salesCount;         // 销售数量
    private Double salesAmount;      // 销售金额

    public BrandTopMetrics() {
    }

    public BrandTopMetrics(Timestamp windowStart, Timestamp windowEnd, Integer rank,
                          String brand, Long salesCount, Double salesAmount) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.rank = rank;
        this.brand = brand;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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
}

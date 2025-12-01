package com.flink.realtime.generator;

import com.flink.realtime.generator.model.OrderDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 订单明细数据生成器
 */
public class OrderDetailGenerator {
    private static final Random random = new Random();
    private static final int PRODUCT_ID_START = 2001;
    private static final int PRODUCT_ID_END = 2200;
    
    // 按分类划分的商品价格区间
    private static final double[] PRICE_RANGES = {99.0, 299.0, 599.0, 999.0, 1999.0, 3999.0};

    public static List<OrderDetail> generate(String orderId, int detailCount, long detailIdSequence) {
        List<OrderDetail> details = new ArrayList<>();
        
        for (int i = 0; i < detailCount; i++) {
            OrderDetail detail = new OrderDetail();
            
            // 生成明细ID (DET + yyyyMMdd + 8位序列号)
            detail.setDetailId(String.format("DET%08d", detailIdSequence + i));
            detail.setOrderId(orderId);
            
            // 生成商品ID (P2001 ~ P2200)
            int productId = PRODUCT_ID_START + random.nextInt(PRODUCT_ID_END - PRODUCT_ID_START + 1);
            detail.setProductId("P" + productId);
            
            // 生成商品数量 (1-5)
            int quantity = 1 + random.nextInt(5);
            detail.setProductNum(quantity);
            
            // 生成商品价格 (折扣系数0.8-1.0)
            double basePrice = PRICE_RANGES[random.nextInt(PRICE_RANGES.length)];
            double discount = 0.8 + random.nextDouble() * 0.2;
            double price = Math.round(basePrice * discount * 100.0) / 100.0;
            detail.setProductPrice(price);
            
            // 计算明细金额
            double amount = Math.round(price * quantity * 100.0) / 100.0;
            detail.setDetailAmount(amount);
            
            details.add(detail);
        }
        
        return details;
    }

    /**
     * 生成无效明细（用于测试数据清洗功能）
     */
    public static OrderDetail generateInvalid(String orderId, long detailIdSequence) {
        OrderDetail detail = new OrderDetail();
        
        int errorType = random.nextInt(3);
        switch (errorType) {
            case 0: // 缺少商品ID
                detail.setDetailId(String.format("DET%08d", detailIdSequence));
                detail.setOrderId(orderId);
                detail.setProductId(null);
                detail.setProductNum(1);
                detail.setProductPrice(100.0);
                detail.setDetailAmount(100.0);
                break;
            case 1: // 无效的商品数量
                detail.setDetailId(String.format("DET%08d", detailIdSequence));
                detail.setOrderId(orderId);
                detail.setProductId("P2001");
                detail.setProductNum(0); // 无效数量
                detail.setProductPrice(100.0);
                detail.setDetailAmount(0.0);
                break;
            case 2: // 负数价格
                detail.setDetailId(String.format("DET%08d", detailIdSequence));
                detail.setOrderId(orderId);
                detail.setProductId("P2001");
                detail.setProductNum(1);
                detail.setProductPrice(-100.0);
                detail.setDetailAmount(-100.0);
                break;
        }
        
        return detail;
    }

    /**
     * 根据明细列表计算订单总额
     */
    public static double calculateTotalAmount(List<OrderDetail> details) {
        double total = 0.0;
        for (OrderDetail detail : details) {
            total += detail.getDetailAmount();
        }
        return Math.round(total * 100.0) / 100.0;
    }
}

package com.flink.realtime.generator;

import com.flink.realtime.generator.model.OrderMain;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 订单主表数据生成器
 */
public class OrderMainGenerator {
    private static final Random random = new Random();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int USER_ID_START = 1001;
    private static final int USER_ID_END = 1100;

    public static OrderMain generate(String orderId, double totalAmount, long timestamp) {
        OrderMain order = new OrderMain();
        order.setOrderId(orderId);
        
        // 生成用户ID (U1001 ~ U1100)
        int userId = USER_ID_START + random.nextInt(USER_ID_END - USER_ID_START + 1);
        order.setUserId("U" + userId);
        
        // 生成订单状态 (1=待支付 10%, 2=已支付 80%, 3=已取消 10%)
        double rand = random.nextDouble();
        int status;
        if (rand < 0.1) {
            status = 1; // 待支付
        } else if (rand < 0.9) {
            status = 2; // 已支付
        } else {
            status = 3; // 已取消
        }
        order.setOrderStatus(status);
        
        // 设置订单总额
        order.setTotalAmount(Math.round(totalAmount * 100.0) / 100.0);
        
        // 设置创建时间
        order.setCreateTime(sdf.format(new Date(timestamp)));
        
        // 设置支付时间（仅限已支付订单）
        if (status == 2) {
            long payDelay = (60 + random.nextInt(240)) * 1000L; // 1-5分钟延迟
            order.setPayTime(sdf.format(new Date(timestamp + payDelay)));
        } else {
            order.setPayTime(null);
        }
        
        return order;
    }

    /**
     * 生成无效订单（用于测试数据清洗功能）
     */
    public static OrderMain generateInvalid(String orderId, long timestamp) {
        OrderMain order = new OrderMain();
        
        int errorType = random.nextInt(4);
        switch (errorType) {
            case 0: // 缺少订单ID
                order.setOrderId(null);
                order.setUserId("U1001");
                order.setOrderStatus(2);
                order.setTotalAmount(100.0);
                order.setCreateTime(sdf.format(new Date(timestamp)));
                break;
            case 1: // 无效的订单状态
                order.setOrderId(orderId);
                order.setUserId("U1001");
                order.setOrderStatus(9); // 无效状态
                order.setTotalAmount(100.0);
                order.setCreateTime(sdf.format(new Date(timestamp)));
                break;
            case 2: // 负数金额
                order.setOrderId(orderId);
                order.setUserId("U1001");
                order.setOrderStatus(2);
                order.setTotalAmount(-100.0);
                order.setCreateTime(sdf.format(new Date(timestamp)));
                break;
            case 3: // 错误的时间格式
                order.setOrderId(orderId);
                order.setUserId("U1001");
                order.setOrderStatus(2);
                order.setTotalAmount(100.0);
                order.setCreateTime("2025/11/16"); // 错误格式
                break;
        }
        
        return order;
    }
}

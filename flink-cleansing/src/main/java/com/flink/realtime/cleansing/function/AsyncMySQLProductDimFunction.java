package com.flink.realtime.cleansing.function;

import com.flink.realtime.cleansing.model.OrderWide;
import com.flink.realtime.cleansing.model.OrderWithUserDim;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步关联MySQL商品维度表
 */
public class AsyncMySQLProductDimFunction extends RichAsyncFunction<OrderWithUserDim, OrderWide> {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMySQLProductDimFunction.class);
    private transient HikariDataSource dataSource;
    private transient ExecutorService executorService;
    
    @Override
    public void open(Configuration parameters) throws Exception {
        // 初始化HikariCP连接池
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://master1:3306/flink_realtime_dw?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        config.setUsername("root");
        config.setPassword("780122");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
        
        // 创建线程池用于异步查询
        executorService = Executors.newFixedThreadPool(10);
        
        logger.info("异步MySQL商品维度关联函数初始化成功");
    }
    
    @Override
    public void asyncInvoke(OrderWithUserDim input, ResultFuture<OrderWide> resultFuture) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT product_name, category, brand FROM product_info WHERE product_id = ?")) {
                
                ps.setString(1, input.getProductId());
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    OrderWide result = new OrderWide();
                    // 复制订单和用户数据
                    result.setOrderId(input.getOrderId());
                    result.setDetailId(input.getDetailId());
                    result.setUserId(input.getUserId());
                    result.setProductId(input.getProductId());
                    result.setProductNum(input.getProductNum());
                    result.setProductPrice(input.getProductPrice());
                    result.setDetailAmount(input.getDetailAmount());
                    result.setOrderStatus(input.getOrderStatus());
                    result.setCreateTime(input.getCreateTime());
                    result.setUserName(input.getUserName());
                    result.setUserLevel(input.getUserLevel());
                    result.setUserCity(input.getUserCity());
                    
                    // 添加商品维度信息
                    result.setProductName(rs.getString("product_name"));
                    result.setCategory(rs.getString("category"));
                    result.setBrand(rs.getString("brand"));
                    
                    return result;
                } else {
                    logger.warn("未找到商品信息，商品ID: {}", input.getProductId());
                    return null;
                }
            } catch (Exception e) {
                logger.error("查询商品维度信息失败，商品ID: {}", input.getProductId(), e);
                return null;
            }
        }, executorService).thenAccept(result -> {
            if (result != null) {
                resultFuture.complete(Collections.singleton(result));
            } else {
                resultFuture.complete(Collections.emptyList());
            }
        });
    }
    
    @Override
    public void timeout(OrderWithUserDim input, ResultFuture<OrderWide> resultFuture) {
        logger.warn("异步查询超时，商品ID: {}", input.getProductId());
        resultFuture.complete(Collections.emptyList());
    }
    
    @Override
    public void close() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        logger.info("异步MySQL商品维度关联函数已关闭");
    }
}

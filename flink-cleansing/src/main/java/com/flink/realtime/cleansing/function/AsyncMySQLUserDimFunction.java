package com.flink.realtime.cleansing.function;

import com.flink.realtime.cleansing.model.OrderJoinResult;
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
 * 异步关联MySQL用户维度表
 */
public class AsyncMySQLUserDimFunction extends RichAsyncFunction<OrderJoinResult, OrderWithUserDim> {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMySQLUserDimFunction.class);
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
        
        logger.info("异步MySQL用户维度关联函数初始化成功");
    }
    
    @Override
    public void asyncInvoke(OrderJoinResult input, ResultFuture<OrderWithUserDim> resultFuture) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT user_name, user_level, city FROM user_info WHERE user_id = ?")) {
                
                ps.setString(1, input.getUserId());
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    OrderWithUserDim result = new OrderWithUserDim();
                    // 复制订单数据
                    result.setOrderId(input.getOrderId());
                    result.setDetailId(input.getDetailId());
                    result.setUserId(input.getUserId());
                    result.setProductId(input.getProductId());
                    result.setProductNum(input.getProductNum());
                    result.setProductPrice(input.getProductPrice());
                    result.setDetailAmount(input.getDetailAmount());
                    result.setOrderStatus(input.getOrderStatus());
                    result.setCreateTime(input.getCreateTime());
                    
                    // 添加用户维度信息
                    result.setUserName(rs.getString("user_name"));
                    result.setUserLevel(rs.getInt("user_level"));
                    result.setUserCity(rs.getString("city"));
                    
                    return result;
                } else {
                    logger.warn("未找到用户信息，用户ID: {}", input.getUserId());
                    return null;
                }
            } catch (Exception e) {
                logger.error("查询用户维度信息失败，用户ID: {}", input.getUserId(), e);
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
    public void timeout(OrderJoinResult input, ResultFuture<OrderWithUserDim> resultFuture) {
        logger.warn("异步查询超时，用户ID: {}", input.getUserId());
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
        logger.info("异步MySQL用户维度关联函数已关闭");
    }
}

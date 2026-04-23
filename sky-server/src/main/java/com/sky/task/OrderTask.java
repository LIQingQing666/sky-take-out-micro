package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理订单超时
     */
    @Scheduled(cron = "0 * * * * ?")
    //@Scheduled(cron = "1/5 * * * * ?")//测试用：每隔5秒执行一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单，每分钟触发一次：{}", LocalDateTime.now());
        //查询处于待付款状态的订单以及创建时间超过15分钟的订单-判定超时
        //select * from orders where status = 1 and order_time < (当前时间 - 15分钟);

        List<Orders> orderList = orderMapper.processTimeoutOrder(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if(orderList != null){
            for(Orders order : orderList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.cancel(order);
            }
        }
    }

    /**
     * 处理一直处于派送状态的订单
     * 每天凌晨一点触发一次
     */
    @Scheduled(cron = "0 0 1 * * ?")
    //@Scheduled(cron = "0/5 * * * * ?")//测试用：每隔5秒执行一次
     public void processDeliveryOrder(){
        log.info("处理一直处于派送状态的订单:{}", LocalDateTime.now());
        List<Orders> orderList = orderMapper.processTimeoutOrder(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if(orderList != null){
            for(Orders order : orderList){
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                orderMapper.complete(order);
            }
        }
     }
}

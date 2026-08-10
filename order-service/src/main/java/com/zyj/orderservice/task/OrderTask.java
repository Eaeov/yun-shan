package com.zyj.orderservice.task;

import com.sky.entity.Orders;
import com.zyj.orderservice.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.task
 * @Project：yun-shan
 * @name：OrderTask
 * @Date：17 12月 2025  15:20
 * @Filename：OrderTask
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {

    private final OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        Integer status = Orders.PENDING_PAYMENT; //待付款
        LocalDateTime orderTime = LocalDateTime.now().plusMinutes(-15); //当前时间减15分钟
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(status ,orderTime);

        if (ordersList != null && ordersList.size() > 0){
            for (Orders order : ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时，自动取消");
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理派送中订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理派送中得订单：{}",LocalDateTime.now());
        Integer status = Orders.DELIVERY_IN_PROGRESS; // 派送中
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-60);//上一天没有处理得订单
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(status, localDateTime);

        if (ordersList != null && ordersList.size() > 0){
            for (Orders order : ordersList){
                order.setStatus(Orders.COMPLETED);//订单完成
                orderMapper.update(order);
            }
        }

    }
}

package com.zyj.userservice.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.entity.Merchant;
import com.sky.entity.Orders;
import com.sky.vo.*;
import com.zyj.userservice.mapper.UserMapper;
import com.zyj.userservice.service.EmployeeService;
import com.zyj.userservice.service.MerchantService;
import com.zyj.userservice.service.WorkSpaceService;
import com.zyj.yunapi.client.OrderClient;
import com.zyj.yunapi.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author：zyj}
 * {@code @Package：com.zyj.userservice.service.impl}
 * {@code @Project：yun-shan}
 * {@code @name：WorkSpaceServiceImpl}
 * {@code @Date：14 5月 2026  22:39}
 * {@code @Filename：WorkSpaceServiceImpl}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkSpaceServiceImpl implements WorkSpaceService {

    private final MerchantService merchantService;
    private final EmployeeService employeeService;
    private final OrderClient orderClient;
    private final UserMapper userMapper;
    private final ProductClient productClient;
    /**
     * 商户数据统计（按商家隔离）
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        //1.查询订单总数
        Integer totalOrderCount = (Integer) orderClient.countByMap(map).getData();
        map.put("status", Orders.COMPLETED);
        //2.查询营业额
        Double turnover = (Double) orderClient.sumByMap(map).getData();
        turnover = turnover == null ? 0 : turnover;
        //3.查询有效订单数
        Integer validOrderCount = (Integer) orderClient.countByMap(map).getData();
        Double orderCompletionRate = 0.0;
        Double unitPrice = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            // 4、订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            // 5、平均客单价
            unitPrice = turnover / validOrderCount;
        }
        // 6、新增用户数
        Integer newUsers = userMapper.countByMap(map);
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 订单数据统计
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        /**
         * 全部订单 待接单 待派送 已完成 已取消
         */
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        Integer allOrders = (Integer) orderClient.countByMap(map).getData();
        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer toConfirmed = (Integer) orderClient.countByMap(map).getData();
        map.put("status", Orders.CONFIRMED);
        Integer toDelivery = (Integer) orderClient.countByMap(map).getData();
        map.put("status", Orders.COMPLETED);
        Integer completed = (Integer) orderClient.countByMap(map).getData();
        map.put("status", Orders.CANCELLED);
        Integer canceled = (Integer) orderClient.countByMap(map).getData();

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .waitingOrders(toConfirmed)
                .deliveredOrders(toDelivery)
                .completedOrders(completed)
                .cancelledOrders(canceled)
                .build();
    }

    /**
     * 菜品数据统计
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView() {
        /**
         * 已起售 已停售
         */
        Integer on = productClient.countStatusDish(1).getData();
        Integer off = productClient.countStatusDish(0).getData();
        return DishOverViewVO.builder()
                .sold(on)
                .discontinued(off)
                .build();
    /**
     * 套餐数据统计
     * @return
     */
    }
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        /**
         * 已起售 已停售
         */
        Integer on = productClient.countStatusSetmeal(1).getData();
        Integer off = productClient.countStatusSetmeal(0).getData();
        return SetmealOverViewVO.builder()
                .sold(on)
                .discontinued(off)
                .build();
    }

    private Employee getCurrentEmployee() {
        return employeeService.getById(BaseContext.getCurrentId());
    }
}

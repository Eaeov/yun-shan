package com.zyj.orderservice.controller.admin;


import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import com.zyj.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.controller.admin
 * @Project：yun-shan
 * @name：OrderController
 * @Date：14 12月 2025  15:53
 * @Filename：OrderController
 */
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/order")
@RestController("adminOrderController")
public class OrderController {
    private final OrderService orderService;

    /**
     * 订单搜索（按商家隔离）
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult<OrderVO>> pageOrderSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult<OrderVO> pageResult = orderService.pageOrderSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计（按商家隔离）
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> orderStatistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.orderStatistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 查询订单详情（校验商家归属）
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> orderDetails(@PathVariable Long id) {
        OrderVO orderVO = orderService.orderDetails(id);
        return Result.success(orderVO);
    }

    /**
     * 接单（校验商家归属+状态）
     */
    @PutMapping("/confirm")
    public Result<String> confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单（校验商家归属+状态）
     * @return
     */
    @PutMapping("/rejection")
    public Result<String> rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单（校验商家归属+状态）
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    public Result<String> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("取消订单的原因:{}", ordersCancelDTO.getCancelReason());
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单（校验商家归属+状态）
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    public Result<String> delivery(@PathVariable Long id) {
        log.info("派送订单:{}", id);
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单（校验商家归属+状态）
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    public Result<String> complete(@PathVariable Long id) {
        log.info("完成订单：{}", id);
        orderService.complete(id);
        return Result.success();
    }

    /**
     * 根据动态条件统计营业额（按商家隔离）
     * @param map 查询条件
     * @return 营业额
     */
    @PostMapping("/sumByMap")
    public Result<Double> sumByMap(@RequestBody Map<String, Object> map) {
        Double sum = orderService.sumByMap(map);
        return Result.success(sum);
    }

    /**
     * 根据动态条件统计订单数量（按商家隔离）
     * @param map
     * @return
     */
    @PostMapping("/countByMap")
    public Result<Integer> countByMap(@RequestBody Map<String, Object> map) {
        Integer count = orderService.countByMap(map);
        return Result.success(count);
    }

    /**
     * 商品销量Top10（按商家隔离）
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/getSalesTop10")
    public Result<List<GoodsSalesDTO>> getSalesTop10(
            @RequestParam String begin,
            @RequestParam String end) {
        List<GoodsSalesDTO> goodsSalesDTOList = orderService.getSalesTop10(begin, end);
        return Result.success(goodsSalesDTOList);
    }
}

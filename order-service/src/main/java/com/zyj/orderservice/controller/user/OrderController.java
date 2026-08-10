package com.zyj.orderservice.controller.user;


import com.zyj.orderservice.Info.PreOrderInfo;
import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.zyj.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.controller.user
 * @Project：yun-shan
 * @name：OrderController
 * @Date：12 12月 2025  21:23
 * @Filename：UserOrderController
 */
@RequiredArgsConstructor
@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
public class OrderController {
    private final OrderService orderService;

    /**
     * 历史订单查询（强制userId归属）
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    public Result<PageResult<OrderVO>> pageHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult<OrderVO> pageResult = orderService.pageOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 用户下单（校验商家营业状态+merchantId一致性）
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付（校验userId归属）
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 查询订单详情（校验userId归属）
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getByIdOrderDetail(@PathVariable Long id) {
        OrderVO orderVO = orderService.getByIdOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单（校验userId归属+状态1或2+cancelType=1）
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    public Result<String> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    /**
     * 再来一单（校验userId归属+传递merchantId）
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    public Result<String> anotherOrder(@PathVariable Long id) {
        orderService.anotherOrder(id);
        return Result.success();
    }

    /**
     * 客户催单（校验userId归属+状态2/3/4+插入催单记录+WebSocket通知）
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    public Result<String> reminder(@PathVariable Long id) {
        log.info("客户催单:{}", id);
        orderService.reminder(id);
        return Result.success();
    }

    /**
     * 预生成订单
     */
    @PostMapping("/preview")
    public Result<PreOrderInfo> previewPreOrder(@RequestBody PreOrderDTO preOrderDTO) {
        PreOrderInfo preOrderInfo = orderService.previewPreOrder(preOrderDTO);
        return Result.success(preOrderInfo);
    }

    /**
     * 确认下单接口，用于将 Redis 中的预订单转成真实订单并支付。
     * 需要 订单号 和 支付参数
     */
    @PostMapping("/confirm")
    public Result<OrderPaymentVO> confirmPreOrder (@RequestBody ConfirmPreOrderDTO confirmPreOrderDTO) {
        OrderPaymentVO orderPaymentVO = orderService.confirmPreOrder(confirmPreOrderDTO);
        return Result.success(orderPaymentVO);
    }
}

package com.zyj.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyj.orderservice.Info.PreOrderInfo;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import java.util.List;
import java.util.Map;

/**
 * 订单业务层接口
 *
 * @Author：zyj
 * @Package：com.zyj.orderservice.service
 * @Project：yun-shan
 * @name：OrderService
 * @Date：12 12月 2025  21:24
 * @Filename：OrderService
 */
public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param ordersSubmitDTO 下单请求参数
     * @return 下单结果视图对象
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO 支付请求参数
     * @return 支付视图对象
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功回调，修改订单状态
     * @param outTradeNo 商户订单号
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端-历史订单分页查询
     * @param ordersPageQueryDTO 分页查询参数
     * @return 分页结果对象
     */
    PageResult<OrderVO> pageOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 用户端-根据订单ID查询订单详情（校验userId归属）
     * @param id 订单ID
     * @return 订单详情视图对象
     */
    OrderVO getByIdOrderDetail(Long id);

    /**
     * 用户端-取消订单
     * @param id 订单ID
     */
    void cancelOrder(Long id);

    /**
     * 用户端-再来一单
     * @param id 原订单ID
     */
    void anotherOrder(Long id);

    /**
     * 管理端-订单条件搜索（按商家隔离）
     * @param ordersPageQueryDTO 分页查询参数
     * @return 分页结果对象
     */
    PageResult<OrderVO> pageOrderSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端-各状态订单数量统计（按商家隔离）
     * @return 统计视图对象
     */
    OrderStatisticsVO orderStatistics();

    /**
     * 管理端-查询订单详情（校验商家归属）
     * @param id 订单ID
     * @return 订单详情视图对象
     */
    OrderVO orderDetails(Long id);

    /**
     * 管理端-接单（校验商家归属+状态）
     * @param ordersConfirmDTO 接单参数
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 管理端-拒单（校验商家归属+状态）
     * @param ordersRejectionDTO 拒单参数
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 管理端-取消订单（校验商家归属+状态）
     * @param ordersCancelDTO 取消参数
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 管理端-派送订单（校验商家归属+状态）
     * @param id 订单ID
     */
    void delivery(Long id);

    /**
     * 管理端-完成订单（校验商家归属+状态）
     * @param id 订单ID
     */
    void complete(Long id);

    /**
     * 用户端-催单
     * @param id 订单ID
     */
    void reminder(Long id);

    /**
     * 根据动态条件统计营业额（按商家隔离）
     * @param map 查询条件
     * @return 营业额
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 根据动态条件统计订单数量（按商家隔离）
     * @param map 查询条件
     * @return 订单数量
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 商品销量Top10（按商家隔离）
     * @param begin 开始时间
     * @param end 结束时间
     * @return 销量Top10列表
     */
    List<GoodsSalesDTO> getSalesTop10(String begin, String end);

    PreOrderInfo previewPreOrder(PreOrderDTO preOrderDTO);

    OrderPaymentVO confirmPreOrder(ConfirmPreOrderDTO confirmPreOrderDTO);
}

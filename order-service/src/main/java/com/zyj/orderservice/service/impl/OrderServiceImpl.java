package com.zyj.orderservice.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.orderservice.Info.PreOrderInfo;
import com.sky.constant.MessageConstant;
import com.sky.context.AuthContext;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.OrderPermissionDeniedException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.*;
import com.zyj.orderservice.mapper.OrderDetailMapper;
import com.zyj.orderservice.mapper.OrderMapper;
import com.zyj.orderservice.mapper.OrderRemindMapper;
import com.zyj.orderservice.service.OrderService;
import com.zyj.orderservice.webSocket.WebSocketServer;
import com.zyj.yunapi.client.CartClient;
import com.zyj.yunapi.client.ProductClient;
import com.zyj.yunapi.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 订单业务层实现类
 *
 * @Author：zyj
 * @Package：com.zyj.orderservice.service.impl
 * @Project：yun-shan
 * @name：OrderServiceImpl
 * @Date：12 12月 2025  21:24
 * @Filename：OrderServiceImpl
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderRemindMapper orderRemindMapper;
    private final UserClient userClient;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final WebSocketServer webSocketServer;
    private final RedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private OrderServiceImpl self;

    private static final String SHOP_STATUS_PREFIX = "shop:status:";

    /**
     * 预订单相关查询专用线程池
     * 避免阻塞式 Feign 调用占用 ForkJoinPool.commonPool()（全局公共池，线程数=CPU核数-1），
     * 高并发时防止公共池被 IO 阻塞占满而拖累整个应用。
     */
    private static final ExecutorService PRE_ORDER_POOL = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "preorder-io");
        t.setDaemon(true); // 守护线程，应用退出时自动销毁
        return t;
    });

    // ==================== 用户端接口 ====================

    /**
     * 用户下单
     * 校验：地址存在、购物车非空、商家营业状态、merchantId一致性
     * 注意：本方法不开启事务，校验性 Feign 调用在事务外执行，避免占用 DB 连接
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // ====== Phase 1: 校验（无事务，Feign 调用不占用 DB 连接）======

        AddressBook addressBook = userClient.getAddressBookById(ordersSubmitDTO.getAddressBookId()).getData();
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        if (ordersSubmitDTO.getMerchantId() == null) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_ID_CAN_NOT_BE_EMPTY);
        }

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS_PREFIX + ordersSubmitDTO.getMerchantId());
        if (shopStatus == null || shopStatus != 1) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_SHOP_CLOSED);
        }

        if (ordersSubmitDTO.getUserId() == null) {
            ordersSubmitDTO.setUserId(BaseContext.getCurrentId());
        }

        // 直连下单模式：调用方显式传入商品明细（AI点餐/预订单确认），不依赖购物车
        if (ordersSubmitDTO.getItems() != null && !ordersSubmitDTO.getItems().isEmpty()) {
            List<OrderDetail> details = buildOrderDetailsFromItems(ordersSubmitDTO.getItems());
            return self.doSubmitInTransaction(ordersSubmitDTO, addressBook, details, true);
        }

        // 购物车模式：从购物车获取商品
        Result<List<ShoppingCart>> cartResult = cartClient.listShoppingCart();
        if (cartResult == null || cartResult.getData() == null || cartResult.getData().isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        List<ShoppingCart> cartList = cartResult.getData();

        for (ShoppingCart cart : cartList) {
            if (!ordersSubmitDTO.getMerchantId().equals(cart.getMerchantId())) {
                throw new ShoppingCartBusinessException("购物车中存在不属于当前商家的商品");
            }
        }

        // ====== Phase 2: 写入（事务内，仅 DB 操作）======
        return self.doSubmitInTransaction(ordersSubmitDTO, addressBook, cartList, false);
    }

    /**
     * 事务内执行下单写入（仅 DB 操作，不含 Feign 调用）
     * 通过 self 代理调用以触发 @Transactional
     */
    @Transactional
    public OrderSubmitVO doSubmitInTransaction(OrdersSubmitDTO dto, AddressBook addressBook,
                                                List<?> dataList, boolean isItemsMode) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);
        orders.setUserId(dto.getUserId());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(IdUtil.getSnowflakeNextIdStr());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail());

        List<OrderDetail> orderDetailList;
        if (isItemsMode) {
            @SuppressWarnings("unchecked")
            List<OrderDetail> details = (List<OrderDetail>) dataList;
            orderDetailList = details;
            BigDecimal totalAmount = orderDetailList.stream()
                    .map(d -> d.getAmount().multiply(BigDecimal.valueOf(d.getNumber())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            orders.setAmount(totalAmount);
        } else {
            @SuppressWarnings("unchecked")
            List<ShoppingCart> cartList = (List<ShoppingCart>) dataList;
            orderDetailList = new ArrayList<>();
            for (ShoppingCart cart : cartList) {
                OrderDetail detail = new OrderDetail();
                BeanUtils.copyProperties(cart, detail);
                detail.setOrderId(orders.getId());
                orderDetailList.add(detail);
            }
        }

        orderMapper.insert(orders);

        for (OrderDetail detail : orderDetailList) {
            detail.setOrderId(orders.getId());
        }
        orderDetailMapper.insert(orderDetailList);

        // 购物车模式下，事务提交后删除购物车
        if (!isItemsMode) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            cartClient.deleteShoppingCart();
                        }
                    }
            );
        }

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();
    }

    /**
     * 从商品明细列表构建 OrderDetail（含 Feign 校验，不含 DB 操作）
     * 并行查询所有商品/套餐信息
     */
    private List<OrderDetail> buildOrderDetailsFromItems(List<OrderItemDTO> items) {
        List<CompletableFuture<OrderDetail>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    if (item.getQuantity() == null || item.getQuantity() <= 0) {
                        throw new OrderBusinessException("商品数量非法");
                    }
                    OrderDetail detail = new OrderDetail();
                    detail.setDishFlavor(item.getDishFlavor());
                    detail.setNumber(item.getQuantity());

                    if (item.getDishId() != null) {
                        DishVO dish = productClient.getDishById(item.getDishId()).getData();
                        if (dish == null || !Objects.equals(dish.getStatus(), 1)) {
                            throw new OrderBusinessException("商品「" + item.getDishId() + "」不存在或已下架");
                        }
                        detail.setDishId(item.getDishId());
                        detail.setName(dish.getName());
                        detail.setImage(dish.getImage());
                        detail.setAmount(dish.getPrice());
                    } else if (item.getSetmealId() != null) {
                        SetmealVO setmeal = productClient.getSetmealById(item.getSetmealId()).getData();
                        if (setmeal == null || !Objects.equals(setmeal.getStatus(), 1)) {
                            throw new OrderBusinessException("套餐「" + item.getSetmealId() + "」不存在或已下架");
                        }
                        detail.setSetmealId(item.getSetmealId());
                        detail.setName(setmeal.getName());
                        detail.setImage(setmeal.getImage());
                        detail.setAmount(setmeal.getPrice());
                    } else {
                        throw new OrderBusinessException("商品ID不能为空");
                    }
                    return detail;
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 订单支付
     * 校验：订单userId归属当前用户
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO){
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查订单，校验归属
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】校验订单归属当前用户
        if (!ordersDB.getUserId().equals(userId)) {
            throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;

        LocalDateTime cheOutTime = LocalDateTime.now();
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        log.info("调用updateStatus，用来替换微信支付来更新数据库状态问题");

        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, cheOutTime, orderNumber);

        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + ordersPaymentDTO.getOrderNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToMerchant(ordersDB.getMerchantId(), json);
        return vo;
    }

    /**
     * 支付成功回调
     */
    @Override
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        String json = JSON.toJSONString(map);
        //webSocketServer.sendToAllClient(json);
        webSocketServer.sendToMerchant(ordersDB.getMerchantId(), json);
    }

    /**
     * 用户端-历史订单分页查询
     * 【权限校验】强制设置userId为当前用户，确保只能看自己订单
     */
    @Override
    public PageResult<OrderVO> pageOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("历史订单查询：{}", ordersPageQueryDTO);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(ordersPageQueryDTO.getUserId() != null, Orders::getUserId, ordersPageQueryDTO.getUserId())
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())
                .orderByDesc(Orders::getOrderTime);
        IPage<Orders> ordersIPage = orderMapper.selectPage(page, wrapper);

        List<OrderVO> orderVOList = fillOrderDetailList(ordersIPage.getRecords());
        log.info("查询结果:{}", orderVOList);
        return new PageResult<>(ordersIPage.getTotal(), orderVOList);
    }

    /**
     * 用户端-订单详情
     * 【权限校验】校验订单userId归属当前用户
     */
    @Override
    public OrderVO getByIdOrderDetail(Long id) {
        log.info("订单详情查询：{}", id);

        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】校验订单归属当前用户
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        List<OrderDetail> orderDetail = orderDetailMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        orderVO.setOrderDetailList(orderDetail);
        BeanUtils.copyProperties(orders, orderVO);
        return orderVO;
    }

    /**
     * 用户端-取消订单
     * 【权限校验】校验userId归属 + 状态校验(1或2) + cancelType=1(用户取消)
     */
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】校验订单归属当前用户
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        // 状态校验：仅允许取消待付款(1)或待接单(2)状态的订单
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setCancelReason("用户取消");
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelType(Orders.CANCEL_BY_USER); // cancel_type=1 用户取消
        orderMapper.update(orders);
    }

    /**
     * 用户端-再来一单
     * 【权限校验】校验原订单userId归属当前用户
     */
    @Override
    @Transactional
    public void anotherOrder(Long id) {
        log.info("再来一单操作，原订单ID：{}", id);

        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】校验订单归属当前用户
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        List<OrderDetail> list = orderDetailMapper.getById(id);
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> cart = new ArrayList<>();
        for (OrderDetail o : list) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(o, shoppingCart);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            shoppingCart.setMerchantId(ordersDB.getMerchantId()); // 设置商家ID
            cart.add(shoppingCart);
        }

        for (ShoppingCart shoppingCart : cart) {
            ShoppingCartDTO dto = new ShoppingCartDTO();
            dto.setDishId(shoppingCart.getDishId());
            dto.setSetmealId(shoppingCart.getSetmealId());
            dto.setDishFlavor(shoppingCart.getDishFlavor());
            dto.setMerchantId(shoppingCart.getMerchantId()); // 传递商家ID
            cartClient.add(dto);
        }
    }

    /**
     * 用户端-催单
     * 【权限校验】校验userId归属 + 状态校验(2/3/4) + 插入催单记录 + WebSocket通知
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】校验订单归属当前用户
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        // 状态校验：仅进行中的订单可催单(2待接单/3已接单/4派送中)
        if (ordersDB.getStatus() < 2 || ordersDB.getStatus() > 4) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_UNDELIVERABLE);
        }

        // 插入催单记录
        OrderRemind orderRemind = OrderRemind.builder()
                .orderId(id)
                .userId(BaseContext.getCurrentId())
                .createTime(LocalDateTime.now())
                .build();
        orderRemindMapper.insert(orderRemind);

        log.info("客户催单:{}", id);
        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号:" + ordersDB.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToMerchant(ordersDB.getMerchantId(), json);
//        webSocketServer.sendToAllClient(json);
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端-订单条件搜索
     * 【权限校验】非超管强制设置merchantId为当前员工所属商家
     */
    @Override
    public PageResult<OrderVO> pageOrderSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单搜索：{}", ordersPageQueryDTO);

        // [TENANT-PLUGIN] 以下 setMerchantId 注入已由 TenantLineInnerInterceptor 自动处理，测试通过后删除
        // if (!AuthContext.isSuperAdmin()) {
        //     ordersPageQueryDTO.setMerchantId(AuthContext.getCurrentMerchantId());
        // }

        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ordersPageQueryDTO.getNumber() != null, Orders::getNumber, ordersPageQueryDTO.getNumber())
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())
                .eq(ordersPageQueryDTO.getPhone() != null, Orders::getPhone, ordersPageQueryDTO.getPhone())
                // [TENANT-PLUGIN] .eq(merchantId) 已由 TenantLineInnerInterceptor 自动处理，测试通过后删除下行注释
                // .eq(ordersPageQueryDTO.getMerchantId() != null, Orders::getMerchantId, ordersPageQueryDTO.getMerchantId())
                .between(ordersPageQueryDTO.getBeginTime() != null && ordersPageQueryDTO.getEndTime() != null,
                        Orders::getOrderTime, ordersPageQueryDTO.getBeginTime(), ordersPageQueryDTO.getEndTime());

        IPage<Orders> ordersIPage = orderMapper.selectPage(page, queryWrapper);
        List<OrderVO> orderVOList = fillOrderDetailList(ordersIPage.getRecords());
        log.info("订单搜索结果：{}", orderVOList.toString());
        return new PageResult<>(ordersIPage.getTotal(), orderVOList);
    }

    /**
     * 管理端-各状态订单数量统计
     * 【权限校验】非超管只统计本商家订单
     */
    @Override
    public OrderStatisticsVO orderStatistics() {
        // [TENANT-PLUGIN] merchaht_id 过滤已由 TenantLineInnerInterceptor 自动处理，
        // 直接使用 countStatus 即可（插件自动追加 WHERE 条件），测试通过后删除下方注释代码
        // Long merchantId = AuthContext.isSuperAdmin() ? null : AuthContext.getCurrentMerchantId();
        // if (merchantId != null) {
        //     toBeConfirmed = orderMapper.countStatusByMerchantId(Orders.TO_BE_CONFIRMED, merchantId);
        //     confirmed = orderMapper.countStatusByMerchantId(Orders.CONFIRMED, merchantId);
        //     deliveryInProgress = orderMapper.countStatusByMerchantId(Orders.DELIVERY_IN_PROGRESS, merchantId);
        // } else {
        //     toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        //     confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        //     deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        // }

        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        log.info("各状态订单数量统计：{}", orderStatisticsVO);
        return orderStatisticsVO;
    }

    /**
     * 管理端-订单详情
     * 【权限校验】校验订单merchantId归属当前员工商家
     */
    @Override
    public OrderVO orderDetails(Long id) {
        log.info("订单详情:{}", id);
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(orders);

        List<OrderDetail> orderDetailList = orderDetailMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        orderVO.setOrderDetailList(orderDetailList);
        BeanUtils.copyProperties(orders, orderVO);
        return orderVO;
    }

    /**
     * 管理端-接单
     * 【权限校验】商家归属 + 状态必须为2(待接单) + 记录operatorId
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单：{}", ordersConfirmDTO);
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(ordersDB);

        // 状态校验：必须为待接单(2)
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orders.setOperatorId(AuthContext.getCurrentEmployee().getId()); // 记录操作人
        orderMapper.update(orders);
    }

    /**
     * 管理端-拒单
     * 【权限校验】商家归属 + 状态必须为2(待接单) + cancelType=2(商家取消) + 记录operatorId
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒单原因：{}，订单ID：{}", ordersRejectionDTO.getRejectionReason(), ordersRejectionDTO.getId());
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(ordersDB);

        // 状态校验：必须为待接单(2)
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersRejectionDTO.getId());
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelType(Orders.CANCEL_BY_MERCHANT); // cancel_type=2 商家取消
        orders.setCancelTime(LocalDateTime.now());
        orders.setOperatorId(AuthContext.getCurrentEmployee().getId()); // 记录操作人

        // 若已支付则标记退款
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
    }

    /**
     * 管理端-取消订单
     * 【权限校验】商家归属 + 状态允许2或3 + cancelType=2(商家取消) + 记录operatorId
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        log.info("管理端取消订单：{}", ordersCancelDTO);
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(ordersDB);

        // 状态校验：允许取消待接单(2)或已接单(3)
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED) && !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelType(Orders.CANCEL_BY_MERCHANT); // cancel_type=2 商家取消
        orders.setOperatorId(AuthContext.getCurrentEmployee().getId()); // 记录操作人

        // 若已支付则标记退款
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
    }

    /**
     * 管理端-派送订单
     * 【权限校验】商家归属 + 状态必须为3(已接单) + 记录operatorId
     */
    @Override
    public void delivery(Long id) {
        log.info("派送订单:{}", id);
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(ordersDB);

        // 状态校验：必须为已接单(3)
        if (!ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .operatorId(AuthContext.getCurrentEmployee().getId()) // 记录操作人
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理端-完成订单
     * 【权限校验】商家归属 + 状态必须为4(派送中) + 记录operatorId
     */
    @Override
    public void complete(Long id) {
        log.info("完成订单:{}", id);
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(ordersDB);

        // 状态校验：必须为派送中(4)
        if (!ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .operatorId(AuthContext.getCurrentEmployee().getId()) // 记录操作人
                .build();
        orderMapper.update(orders);
    }

    // ==================== 统计接口 ====================

    /**
     * 根据动态条件统计营业额
     * 【权限校验】非超管强制放入当前员工merchantId
     */
    @Override
    public Double sumByMap(Map<String, Object> map) {
        // [TENANT-PLUGIN] merchan_id 注入已由 TenantLineInnerInterceptor 自动处理，测试通过后删除
        // if (!AuthContext.isSuperAdmin()) {
        //     Object o = map.get("merchantId");
        //     if (o == null) map.put("merchantId", AuthContext.getCurrentMerchantId());
        // }
        return orderMapper.sumByMap(map);
    }

    /**
     * 根据动态条件统计订单数量
     * 【权限校验】非超管强制放入当前员工merchantId
     */
    @Override
    public Integer countByMap(Map<String, Object> map) {
        // [TENANT-PLUGIN] merchan_id 注入已由 TenantLineInnerInterceptor 自动处理，测试通过后删除
        // if (!AuthContext.isSuperAdmin()) {
        //     Object o = map.get("merchantId");
        //     if (o == null) map.put("merchantId", AuthContext.getCurrentMerchantId());
        // }
        return orderMapper.countByMap(map);
    }

    /**
     * 商品销量Top10
     * 【权限校验】非超管强制使用当前员工merchantId
     */
    @Override
    public List<GoodsSalesDTO> getSalesTop10(String beginStr, String endStr) {
        LocalDateTime begin = LocalDateTime.parse(beginStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime end = LocalDateTime.parse(endStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);

        // [TENANT-PLUGIN] merchan_id 注入已由 TenantLineInnerInterceptor 自动处理，测试通过后删除
        // if (!AuthContext.isSuperAdmin()) {
        //     Object o = map.get("merchantId");
        //     if (o == null) map.put("merchantId", AuthContext.getCurrentMerchantId());
        // }

        return orderMapper.getSalesTop10(map);
    }

    /**
     * 生成预订单预览
     */
    @Override
    public PreOrderInfo previewPreOrder(PreOrderDTO preOrderDTO) {
        String preOrderId = "PRE_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);

        // 1. 并行发起查询商家、地址、所有商品信息（任务并行执行，结果按业务顺序取用）
        CompletableFuture<MerchantVO> merchantFuture = CompletableFuture.supplyAsync(
                () -> userClient.getMerchantVOById(preOrderDTO.getMerchantId()).getData(),
                PRE_ORDER_POOL
        );

        CompletableFuture<AddressBook> addressFuture = CompletableFuture.supplyAsync(
                () -> userClient.getAddressBookById(preOrderDTO.getAddressBookId()).getData(),
                PRE_ORDER_POOL
        );

        List<CompletableFuture<PreOrderItemVO>> itemFutures = preOrderDTO.getItems().stream()
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    if (item.getDishId() != null) {
                        DishVO dish = productClient.getDishById(item.getDishId()).getData();
                        if (dish == null || !Objects.equals(dish.getStatus(), 1)) {
                            throw new OrderBusinessException(MessageConstant.NO_COMMODITY_EXISTS);
                        }
                        return PreOrderItemVO.builder()
                                .dishId(item.getDishId())
                                .name(dish.getName())
                                .image(dish.getImage())
                                .quantity(item.getQuantity())
                                .amount(dish.getPrice())
                                .dishFlavor(item.getDishFlavor())
                                .build();
                    } else if (item.getSetmealId() != null) {
                        SetmealVO setmeal = productClient.getSetmealById(item.getSetmealId()).getData();
                        if (setmeal == null || !Objects.equals(setmeal.getStatus(), 1)) {
                            throw new OrderBusinessException(MessageConstant.NO_COMMODITY_EXISTS);
                        }
                        return PreOrderItemVO.builder()
                                .setmealId(item.getSetmealId())
                                .name(setmeal.getName())
                                .image(setmeal.getImage())
                                .quantity(item.getQuantity())
                                .amount(setmeal.getPrice())
                                .dishFlavor(item.getDishFlavor())
                                .build();
                    } else {
                        throw new OrderBusinessException("商品ID不能为空");
                    }
                }, PRE_ORDER_POOL))
                .collect(Collectors.toList());

        // 2. 校验商家（await 会解包异步异常，保持业务异常类型不变；先于其他校验）
        MerchantVO merchant = await(merchantFuture);
        if (merchant == null) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_ID_CAN_NOT_BE_EMPTY);
        }
        if (!Objects.equals(merchant.getStatus(), 1)) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_SHOP_CLOSED);
        }

        // 3. 校验地址
        AddressBook address = await(addressFuture);
        if (address == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 4. 收集商品明细并计算总金额（await 按商品顺序解包异常，报错顺序稳定）
        List<PreOrderItemVO> itemVOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CompletableFuture<PreOrderItemVO> future : itemFutures) {
            PreOrderItemVO vo = await(future);
            itemVOs.add(vo);
            totalAmount = totalAmount.add(vo.getAmount().multiply(BigDecimal.valueOf(vo.getQuantity())));
        }

        // 5. 组装 PreOrderInfo
        PreOrderInfo info = PreOrderInfo.builder()
                .preOrderId(preOrderId)
                .merchantId(preOrderDTO.getMerchantId())
                .userId(BaseContext.getCurrentId())
                .addressBookId(preOrderDTO.getAddressBookId())
                .merchantName(merchant.getName())
                .consignee(address.getConsignee())
                .phone(address.getPhone())
                .address(address.getProvinceName() + address.getCityName()
                        + address.getDistrictName() + address.getDetail())
                .items(itemVOs)
                .amount(totalAmount)
                .packAmount(0)
                .remark(preOrderDTO.getRemark())
                .expireTime(expireTime)
                .build();

        // 6. 存入 Redis
        redisTemplate.opsForValue().set("preorder:" + preOrderId, info, 15, TimeUnit.MINUTES);

        return info;
    }

    /**
     * 确认预订单 生成具体订单并完成支付
     * @param confirmPreOrderDTO
     * @return
     */
    @Transactional
    @Override
    public OrderPaymentVO confirmPreOrder(ConfirmPreOrderDTO confirmPreOrderDTO) {
        String preOrderId = confirmPreOrderDTO.getPreOrderId();
        String redisKey = "preorder:" + preOrderId;

        // 1. 原子性地取出并删除预订单，防止并发重复下单
        PreOrderInfo preOrderInfo = (PreOrderInfo) redisTemplate.opsForValue().getAndDelete(redisKey);
        if (preOrderInfo == null) {
            throw new OrderBusinessException("预订单不存在或已过期");
        }

        // 2. 检查过期（getAndDelete已删除，无需再delete）
        if (preOrderInfo.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new OrderBusinessException("预订单已过期");
        }

        // 3. 校验用户归属
        Long currentUserId = BaseContext.getCurrentId();
        if (!currentUserId.equals(preOrderInfo.getUserId())) {
            throw new OrderPermissionDeniedException("无权操作此预订单");
        }

        // 4. 并行校验商家营业状态 & 商品有效性（快速失败；最终以 submitOrder 重新查库校验为准）
        CompletableFuture<MerchantVO> merchantFuture = CompletableFuture.supplyAsync(
                () -> userClient.getMerchantVOById(preOrderInfo.getMerchantId()).getData(),
                PRE_ORDER_POOL
        );

        List<CompletableFuture<Void>> itemFutures = preOrderInfo.getItems().stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    if (item.getDishId() != null) {
                        DishVO dish = productClient.getDishById(item.getDishId()).getData();
                        if (dish == null || !Objects.equals(dish.getStatus(), 1)) {
                            throw new OrderBusinessException("商品「" + item.getName() + "」已下架");
                        }
                    } else if (item.getSetmealId() != null) {
                        SetmealVO setmeal = productClient.getSetmealById(item.getSetmealId()).getData();
                        if (setmeal == null || !Objects.equals(setmeal.getStatus(), 1)) {
                            throw new OrderBusinessException("套餐「" + item.getName() + "」已下架");
                        }
                    }
                }, PRE_ORDER_POOL))
                .collect(Collectors.toList());

        // 等待所有校验完成（await 解包异步异常，报错顺序稳定：先商家、后商品）
        MerchantVO merchant = await(merchantFuture);
        if (merchant == null || !Objects.equals(merchant.getStatus(), 1)) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_SHOP_CLOSED);
        }
        for (CompletableFuture<Void> future : itemFutures) {
            await(future);
        }

        // 5. 构建下单DTO（items来自预订单，不再依赖购物车）
        List<OrderItemDTO> orderItems = preOrderInfo.getItems().stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setDishId(item.getDishId());
            dto.setSetmealId(item.getSetmealId());
            dto.setDishFlavor(item.getDishFlavor());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        OrdersSubmitDTO ordersSubmitDTO = OrdersSubmitDTO.builder()
                .userId(currentUserId)
                .merchantId(preOrderInfo.getMerchantId())
                .addressBookId(preOrderInfo.getAddressBookId())
                .remark(preOrderInfo.getRemark())
                .packAmount(preOrderInfo.getPackAmount() != null ? preOrderInfo.getPackAmount() : 0)
                // 注意：不传 amount，submitOrder 的 items 模式会按数据库最新价格重新计价，
                // 避免使用预览时的旧价格快照
                .items(orderItems)
                .build();

        // 6. 下单
        try {
            OrderSubmitVO result = this.submitOrder(ordersSubmitDTO);
            if (result == null || result.getOrderNumber() == null) {
                throw new RuntimeException("创建订单失败");
            }
            String orderNumber = result.getOrderNumber();

            // 7. 支付
            OrdersPaymentDTO paymentDTO = new OrdersPaymentDTO();
            paymentDTO.setOrderNumber(orderNumber);
            paymentDTO.setPayMethod(1);

            return this.payment(paymentDTO);

        } catch (Exception e) {
            // 下单或支付失败，回补预订单让用户可重试
            long remainingSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                    LocalDateTime.now(), preOrderInfo.getExpireTime());
            if (remainingSeconds > 0) {
                redisTemplate.opsForValue().set(redisKey, preOrderInfo,
                        remainingSeconds, TimeUnit.SECONDS);
            }
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 等待异步任务完成并取结果。
     * CompletableFuture 会把子任务抛出的异常包装为 CompletionException，
     * future.get() 再包装为 ExecutionException，这里循环解包，恢复抛出的业务异常类型
     * （如 OrderBusinessException），保证全局异常处理器能正确识别。
     */
    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (ExecutionException | CompletionException e) {
            throw unwrap(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("异步任务被中断", e);
        }
    }

    /**
     * 循环解包异步异常，保留原始业务异常（否则全局异常处理器匹配不到业务异常类型）
     */
    private static RuntimeException unwrap(Throwable t) {
        Throwable cause = t;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(cause);
    }

    /**
     * 校验订单商家归属权限
     * 非超管时，订单的merchantId必须等于当前员工的merchantId
     * @param orders 订单对象
     */
    private void checkMerchantPermission(Orders orders) {
        if (!AuthContext.isSuperAdmin()) {
            Long currentMerchantId = AuthContext.getCurrentMerchantId();
            if (currentMerchantId == null || !currentMerchantId.equals(orders.getMerchantId())) {
                throw new OrderPermissionDeniedException(MessageConstant.ORDER_PERMISSION_DENIED);
            }
        }
    }

    /**
     * 批量装配订单明细：IN 查询 + 内存分组，消除 N+1 查询
     */
    private List<OrderVO> fillOrderDetailList(List<Orders> ordersList) {
        if (ordersList == null || ordersList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> orderIds = ordersList.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderDetail> details = orderDetailMapper.getByOrderIds(orderIds);
        Map<Long, List<OrderDetail>> detailMap = details.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        return ordersList.stream().map(o -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(o, vo);
            vo.setOrderDetailList(detailMap.getOrDefault(o.getId(), new ArrayList<>()));
            return vo;
        }).collect(Collectors.toList());
    }
}
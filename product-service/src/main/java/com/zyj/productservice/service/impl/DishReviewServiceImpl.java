package com.zyj.productservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.AuthContext;
import com.sky.dto.DishReviewPageQueryDTO;
import com.sky.dto.DishReviewSubmitDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishReview;
import com.sky.entity.OrderDetail;
import com.sky.exception.BusinessException;
import com.sky.result.PageResult;
import com.sky.vo.DishReviewVO;
import com.sky.vo.OrderVO;
import com.zyj.productservice.mapper.DishMapper;
import com.zyj.productservice.mapper.DishReviewMapper;
import com.zyj.productservice.service.DishReviewService;
import com.zyj.yunapi.client.OrderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品评论 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishReviewServiceImpl extends ServiceImpl<DishReviewMapper, DishReview> implements DishReviewService {

    private final DishReviewMapper dishReviewMapper;
    private final DishMapper dishMapper;
    private final OrderClient orderClient;

    // ==================== 用户端 ====================

    /**
     * 用户端-提交评论
     * 校验：
     * 1. 当前用户已登录
     * 2. 订单属于该用户、状态已完成、且明细包含该菜品
     * 3. 未重复评论（同一用户+同一订单+同一菜品）
     */
    @Override
    @Transactional
    public void submitReview(DishReviewSubmitDTO dto) {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(MessageConstant.USER_NOT_LOGIN);
        }
        log.info("用户[{}]提交菜品评论: dishId={}, orderId={}, rating={}", currentUserId, dto.getDishId(), dto.getOrderId(), dto.getRating());

        // 评分校验
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        // 【购买校验】Feign 调用 order-service 查询订单详情
        OrderVO orderVO;
        try {
            orderVO = orderClient.getOrderDetail(dto.getOrderId()).getData();
        } catch (Exception e) {
            log.error("查询订单详情失败，orderId={}", dto.getOrderId(), e);
            throw new BusinessException("查询订单信息失败");
        }

        if (orderVO == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 校验订单归属
        if (!orderVO.getUserId().equals(currentUserId)) {
            throw new BusinessException(MessageConstant.ORDER_PERMISSION_DENIED);
        }

        // 校验订单状态 = 已完成(5)
        if (orderVO.getStatus() == null || !orderVO.getStatus().equals(5)) {
            throw new BusinessException(MessageConstant.ORDER_NOT_COMPLETED);
        }

        // 校验订单明细中是否包含该菜品
        List<OrderDetail> orderDetails = orderVO.getOrderDetailList();
        if (orderDetails == null || orderDetails.isEmpty()) {
            throw new BusinessException("订单无菜品明细");
        }
        boolean containsDish = orderDetails.stream()
                .anyMatch(od -> dto.getDishId().equals(od.getDishId()));
        if (!containsDish) {
            throw new BusinessException("该订单不包含此菜品");
        }

        // 【重复评论校验】同一用户+同一订单+同一菜品只能评论一次
        int count = dishReviewMapper.countByUserAndOrderAndDish(currentUserId, dto.getOrderId(), dto.getDishId());
        if (count > 0) {
            throw new BusinessException(MessageConstant.REVIEW_ALREADY_EXISTS);
        }

        // 构建实体并保存
        String imagesStr = null;
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            imagesStr = String.join(",", dto.getImages());
        }

        DishReview review = DishReview.builder()
                .dishId(dto.getDishId())
                .userId(currentUserId)
                .orderId(dto.getOrderId())
                .rating(dto.getRating())
                .content(dto.getContent())
                .images(imagesStr)
                .isAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : 0)
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        dishReviewMapper.insert(review);
        log.info("菜品评论提交成功，reviewId={}", review.getId());
    }

    /**
     * 用户端-删除评论（逻辑删除：status=0）
     * 校验：评论的 user_id 必须等于当前用户
     */
    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(MessageConstant.USER_NOT_LOGIN);
        }
        log.info("用户[{}]删除评论，reviewId={}", currentUserId, reviewId);

        DishReview review = dishReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评论不存在");
        }

        // 【权限校验】只有自己的评论可以删除
        if (!review.getUserId().equals(currentUserId)) {
            throw new BusinessException(MessageConstant.REVIEW_PERMISSION_DENIED);
        }

        // 逻辑删除
        review.setStatus(0);
        review.setUpdateTime(LocalDateTime.now());
        dishReviewMapper.updateById(review);
        log.info("评论[{}]已删除", reviewId);
    }

    /**
     * 用户端-分页查询菜品评论
     * 只返回 status=1 的评论，匿名用户脱敏
     */
    @Override
    public PageResult<DishReviewVO> pageUserReviews(DishReviewPageQueryDTO dto) {
        log.info("用户端查询菜品评论，dishId={}, page={}, pageSize={}", dto.getDishId(), dto.getPage(), dto.getPageSize());

        Page<DishReview> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<DishReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishReview::getDishId, dto.getDishId())
                .eq(DishReview::getStatus, 1)
                .orderByDesc(DishReview::getCreateTime);

        Page<DishReview> reviewPage = dishReviewMapper.selectPage(page, queryWrapper);
        List<DishReviewVO> voList = reviewsToVOs(reviewPage.getRecords());
        return new PageResult<>(reviewPage.getTotal(), voList);
    }

    // ==================== 商家端 ====================

    /**
     * 商家端-分页查询本商家菜品评论
     * 非超管自动按当前员工 merchantId 过滤
     */
    @Override
    public PageResult<DishReviewVO> pageAdminReviews(DishReviewPageQueryDTO dto) {
        // 【权限校验】获取商家ID，非超管强制过滤
        Long merchantId = resolveMerchantId(dto.getMerchantId());
        log.info("商家端查询评论，merchantId={}, dishId={}, page={}, pageSize={}", merchantId, dto.getDishId(), dto.getPage(), dto.getPageSize());

        if (merchantId == null && !AuthContext.isSuperAdmin()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        // 获取该商家所有菜品ID
        List<Long> dishIds;
        if (dto.getDishId() != null) {
            dishIds = Collections.singletonList(dto.getDishId());
        } else {
            // 查询该商家所有菜品ID
            LambdaQueryWrapper<Dish> dishQuery = new LambdaQueryWrapper<>();
            dishQuery.select(Dish::getId).eq(Dish::getMerchantId, merchantId);
            dishIds = dishMapper.selectObjs(dishQuery).stream()
                    .map(o -> (Long) o)
                    .collect(Collectors.toList());
        }

        if (dishIds.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        Page<DishReview> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<DishReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishReview::getDishId, dishIds)
                .eq(DishReview::getStatus, 1)
                .orderByDesc(DishReview::getCreateTime);

        Page<DishReview> reviewPage = dishReviewMapper.selectPage(page, queryWrapper);
        List<DishReviewVO> voList = reviewsToVOs(reviewPage.getRecords());
        return new PageResult<>(reviewPage.getTotal(), voList);
    }

    /**
     * 商家端-回复/修改回复评论
     * 校验：评论对应的菜品 merchantId 必须等于当前员工的 merchantId
     */
    @Override
    @Transactional
    public void replyReview(Long reviewId, String replyContent) {
        com.sky.entity.Employee currentEmployee = AuthContext.getCurrentEmployee();
        if (currentEmployee == null) {
            throw new BusinessException(MessageConstant.USER_NOT_LOGIN);
        }
        log.info("员工[{}]回复评论，reviewId={}, 内容={}", currentEmployee.getId(), reviewId, replyContent);

        if (replyContent == null || replyContent.trim().isEmpty()) {
            throw new BusinessException("回复内容不能为空");
        }

        DishReview review = dishReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评论不存在");
        }

        // 【权限校验】查询该评论对应菜品的商家归属
        Dish dish = dishMapper.selectById(review.getDishId());
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }

        if (!AuthContext.isSuperAdmin()) {
            Long currentMerchantId = AuthContext.getCurrentMerchantId();
            if (currentMerchantId == null || !currentMerchantId.equals(dish.getMerchantId())) {
                throw new BusinessException(MessageConstant.MERCHANT_PERMISSION_DENIED);
            }
        }

        // 更新回复内容
        review.setReplyContent(replyContent.trim());
        review.setReplyTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        dishReviewMapper.updateById(review);
        log.info("评论[{}]回复成功", reviewId);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析merchantId
     */
    private Long resolveMerchantId(Long inputMerchantId) {
        if (!AuthContext.isSuperAdmin()) {
            return AuthContext.getCurrentMerchantId();
        }
        return inputMerchantId;
    }

    /**
     * DishReview 列表转为 DishReviewVO 列表
     * 匿名用户脱敏处理
     */
    private List<DishReviewVO> reviewsToVOs(List<DishReview> reviews) {
        List<DishReviewVO> voList = new ArrayList<>();
        for (DishReview review : reviews) {
            Dish dish = dishMapper.selectById(review.getDishId());
            String dishName = dish != null ? dish.getName() : "未知菜品";

            List<String> imageList = Collections.emptyList();
            if (review.getImages() != null && !review.getImages().isEmpty()) {
                imageList = Arrays.asList(review.getImages().split(","));
            }

            DishReviewVO vo = DishReviewVO.builder()
                    .id(review.getId())
                    .userId(review.getUserId())
                    .dishId(review.getDishId())
                    .dishName(dishName)
                    .orderId(review.getOrderId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .images(imageList)
                    .isAnonymous(review.getIsAnonymous())
                    .replyContent(review.getReplyContent())
                    .replyTime(review.getReplyTime())
                    .createTime(review.getCreateTime())
                    .build();

            // 匿名脱敏
            if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1) {
                vo.setUserName("匿名用户");
                vo.setUserAvatar("https://img.alicdn.com/imgextra/default-avatar.png");
            } else {
                // TODO: 通过 user-service Feign 获取用户名称和头像，或从其他缓存获取
                vo.setUserName("用户" + review.getUserId());
                vo.setUserAvatar("https://img.alicdn.com/imgextra/default-avatar.png");
            }

            voList.add(vo);
        }
        return voList;
    }
}

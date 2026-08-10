package com.zyj.productservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishReviewPageQueryDTO;
import com.sky.dto.DishReviewSubmitDTO;
import com.sky.entity.DishReview;
import com.sky.result.PageResult;
import com.sky.vo.DishReviewVO;

/**
 * 菜品评论 Service 接口
 */
public interface DishReviewService extends IService<DishReview> {

    /**
     * 用户端-提交评论（含购买校验+重复评论校验）
     * @param dto 评论提交参数
     */
    void submitReview(DishReviewSubmitDTO dto);

    /**
     * 用户端-删除评论（逻辑删除，status=0）
     * @param reviewId 评论ID
     */
    void deleteReview(Long reviewId);

    /**
     * 用户端-分页查询菜品评论
     * @param dto 分页参数
     * @return 分页评论列表
     */
    PageResult<DishReviewVO> pageUserReviews(DishReviewPageQueryDTO dto);

    /**
     * 商家端-分页查询本商家菜品评论（含权限过滤）
     * @param dto 分页参数
     * @return 分页评论列表
     */
    PageResult<DishReviewVO> pageAdminReviews(DishReviewPageQueryDTO dto);

    /**
     * 商家端-回复/修改回复评论
     * @param reviewId 评论ID
     * @param replyContent 回复内容
     */
    void replyReview(Long reviewId, String replyContent);
}

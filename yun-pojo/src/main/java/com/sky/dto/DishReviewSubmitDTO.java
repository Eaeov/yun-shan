package com.sky.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜品评论提交请求
 */
@Data
public class DishReviewSubmitDTO {

    //菜品ID
    private Long dishId;

    //订单ID（已完成的订单）
    private Long orderId;

    //评分 1-5
    private Integer rating;

    //评论内容
    private String content;

    //评论图片URL列表
    private List<String> images;

    //是否匿名 0:实名 1:匿名
    private Integer isAnonymous;
}

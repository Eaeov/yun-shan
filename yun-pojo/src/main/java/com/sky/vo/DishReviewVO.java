package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜品评论视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //评论ID
    private Long id;

    //用户ID
    private Long userId;

    //用户名（匿名时返回"匿名用户"）
    private String userName;

    //用户头像（匿名时返回默认图）
    private String userAvatar;

    //菜品ID
    private Long dishId;

    //菜品名称
    private String dishName;

    //订单ID
    private Long orderId;

    //评分 1-5
    private Integer rating;

    //评论内容
    private String content;

    //评论图片列表
    private List<String> images;

    //是否匿名
    private Integer isAnonymous;

    //商家回复内容
    private String replyContent;

    //商家回复时间
    private LocalDateTime replyTime;

    //评论时间
    private LocalDateTime createTime;
}

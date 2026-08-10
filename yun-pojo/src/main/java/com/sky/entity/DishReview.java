package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品评论实体
 *
 * @Author：zyj
 * @Package：com.sky.entity
 * @Project：yun-shan
 * @name：DishReview
 * @Date：29 4月 2026  09:09
 * @Filename：DishReview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dish_review")
public class DishReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    //菜品ID
    private Long dishId;

    //用户ID
    private Long userId;

    //订单ID（用于验证已消费）
    private Long orderId;

    //评分 1-5
    private Integer rating;

    //评论内容
    private String content;

    //评论图片，逗号分隔URL
    private String images;

    //状态 0:隐藏（已删除） 1:显示
    private Integer status;

    //是否匿名 0:实名 1:匿名
    private Integer isAnonymous;

    //商家回复内容
    private String replyContent;

    //商家回复时间
    private LocalDateTime replyTime;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;
}

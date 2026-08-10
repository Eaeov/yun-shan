package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜品评论分页查询
 */
@Data
public class DishReviewPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    //菜品ID
    private Long dishId;

    //商家ID（商家端由后端注入，用户端不需要）
    private Long merchantId;
}

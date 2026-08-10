package com.sky.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class PreOrderItemVO implements Serializable {
    private Long dishId;           // 菜品ID（如果是菜品）
    private Long setmealId;        // 套餐ID（如果是套餐）
    private String name;           // 商品名称
    private String image;          // 商品图片
    private Integer quantity;      // 数量
    private BigDecimal amount;     // 单价
    private String dishFlavor;     // 口味
}

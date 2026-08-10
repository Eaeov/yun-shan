package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {
    private Long dishId;        // 菜品ID
    private Long setmealId;     // 套餐ID
    private Integer quantity;   // 数量
    private String dishFlavor;  // 口味（如“中辣”）
}
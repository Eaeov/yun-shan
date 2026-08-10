package com.sky.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class PreOrderItemDTO implements Serializable {
    private Long dishId;        // 菜品ID
    private Long setmealId;     // 套餐ID
    private String dishFlavor;  // 口味描述
    private Integer quantity;   // 数量

}

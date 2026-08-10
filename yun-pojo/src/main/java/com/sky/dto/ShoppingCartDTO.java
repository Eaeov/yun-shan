package com.sky.dto;


import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDTO implements Serializable {

    private Long dishId; // 菜品ID
    private Long setmealId; // 套餐ID
    private String dishFlavor; // 菜品口味
    private Long merchantId; // 商家ID

}

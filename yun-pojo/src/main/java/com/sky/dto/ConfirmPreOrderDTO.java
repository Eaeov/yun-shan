package com.sky.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ConfirmPreOrderDTO implements Serializable {
    @NotNull
    private String preOrderId;   // 预订单ID
    @NotNull
    private Integer payMethod;   // 支付方式
}
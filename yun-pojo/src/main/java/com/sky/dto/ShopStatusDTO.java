package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShopStatusDTO implements Serializable {

    private Long merchantId;

    //1营业中 0打烊中
    private Integer status;
}

package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantDTO implements Serializable {

    private Long id;

    private String name;

    private String logo;

    private String description;

    //状态 0:停用 1:启用
    private Integer status;
}

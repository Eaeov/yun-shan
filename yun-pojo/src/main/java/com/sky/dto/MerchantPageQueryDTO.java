package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantPageQueryDTO implements Serializable {

    private String name;

    private Integer status;

    private int page;

    private int pageSize;
}

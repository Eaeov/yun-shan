package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantVO implements Serializable {

    private static final long serialVersionUID = 1L; //

    private Long id; // 商家ID

    private String name; // 商家名称

    private String logo; // 商家logo

    private String description; // 商家描述

    private Integer status; // 商家状态

    private LocalDateTime createTime; // 创建时间
}

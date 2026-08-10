package com.sky.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PreOrderDTO implements Serializable {
    private Long merchantId;           // 商家ID（必填）
    private Long addressBookId;        // 地址簿ID（必填）
    private List<PreOrderItemDTO> items; // 商品列表（必填）
    private String remark;             // 备注
    private Integer packAmount;        // 打包费
    private BigDecimal amount;         // 总金额
}

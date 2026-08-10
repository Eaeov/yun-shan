package com.zyj.orderservice.Info;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.sky.dto.PreOrderDTO;
import com.sky.dto.PreOrderItemDTO;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import com.sky.vo.PreOrderItemVO;
import com.sky.vo.SetmealVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.zyj.yunapi.client.ProductClient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class PreOrderInfo implements Serializable {
    @JsonPropertyDescription("预订单id")
    private String preOrderId;              // 预订单ID（Redis key）
    @JsonPropertyDescription("商家id")
    private Long merchantId;                // 商家ID
    @JsonPropertyDescription("商家名称")
    private String merchantName;            // 商家名称
    @JsonPropertyDescription("收货人")
    private String consignee;               // 收货人
    @JsonPropertyDescription("联系电话")
    private String phone;                   // 联系电话
    @JsonPropertyDescription("配送地址")
    private String address;                 // 配送地址
    @JsonPropertyDescription("商品明细")
    private List<PreOrderItemVO> items;     // 商品明细
    @JsonPropertyDescription("总金额")
    private BigDecimal amount;              // 总金额
    @JsonPropertyDescription("打包费")
    private Integer packAmount;             // 打包费
    @JsonPropertyDescription("备注")
    private String remark;                  // 备注
    @JsonPropertyDescription("下单用户ID")
    private Long userId;          // 下单用户ID
    @JsonPropertyDescription("地址簿ID（用于确认下单时回传）")
    private Long addressBookId;   // 地址簿ID（用于确认下单时回传）
    @JsonPropertyDescription("过期时间")
    private LocalDateTime expireTime;       // 过期时间


    /**
     * 从预订单DTO转换为预订单VO（金额从分转元）
     * 注意：此方法需要通过 ProductClient 查询商品详情
     * @param preOrderDTO 预订单DTO（金额单位为分）
     * @param productClient 商品查询客户端
     * @return 预订单VO（金额单位为元）
     */
    public static PreOrderInfo of(PreOrderDTO preOrderDTO, ProductClient productClient) {
        if (preOrderDTO == null) {
            return null;
        }
        // 构建商品明细列表
        List<PreOrderItemVO> items = Optional.ofNullable(preOrderDTO.getItems())
                .orElse(List.of())
                .stream()
                .map(item -> buildPreOrderItemVO(item, productClient))
                .filter(item -> item != null)
                .collect(Collectors.toList());

        return PreOrderInfo.builder()
                .merchantId(preOrderDTO.getMerchantId()) // 商家ID
                .items(items) // 商品明细
                .amount(preOrderDTO.getAmount() != null ? preOrderDTO.getAmount().divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO)
                .packAmount(preOrderDTO.getPackAmount())
                .remark(preOrderDTO.getRemark())
                .build();
    }

    /**
     * 构建单个商品明细项
     */
    private static PreOrderItemVO buildPreOrderItemVO(PreOrderItemDTO item, ProductClient productClient) {
        try {
            // 判断是菜品还是套餐
            if (item.getDishId() != null) {
                // 查询菜品详情
                Result<DishVO> dishResult = productClient.getDishById(item.getDishId());
                if (dishResult != null && dishResult.getData() != null) {
                    DishVO dish = dishResult.getData();
                    return PreOrderItemVO.builder()
                            .dishId(item.getDishId())
                            .name(dish.getName())
                            .image(dish.getImage())
                            .amount(dish.getPrice())
                            .quantity(item.getQuantity())
                            .dishFlavor(item.getDishFlavor())
                            .build();
                }
            } else if (item.getSetmealId() != null) {
                // 查询套餐详情
                Result<SetmealVO> setmealResult = productClient.getSetmealById(item.getSetmealId());
                if (setmealResult != null && setmealResult.getData() != null) {
                    SetmealVO setmeal = setmealResult.getData();
                    return PreOrderItemVO.builder()
                            .setmealId(item.getSetmealId())
                            .name(setmeal.getName())
                            .image(setmeal.getImage())
                            .amount(setmeal.getPrice())
                            .quantity(item.getQuantity())
                            .dishFlavor(item.getDishFlavor())
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("查询商品详情失败，dishId: {}, setmealId: {}", item.getDishId(), item.getSetmealId(), e);
        }

        // 如果查询失败，返回基本信息
        return PreOrderItemVO.builder()
                .quantity(item.getQuantity())
                .dishFlavor(item.getDishFlavor())
                .build();
    }
}

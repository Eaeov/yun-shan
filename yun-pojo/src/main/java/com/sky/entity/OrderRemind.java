package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单催单记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_remind")
public class OrderRemind implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    //订单ID
    private Long orderId;

    //用户ID
    private Long userId;

    //催单时间
    private LocalDateTime createTime;
}

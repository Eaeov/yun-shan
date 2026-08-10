package com.zyj.orderservice.mapper;

import com.sky.entity.OrderRemind;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRemindMapper {

    /**
     * 插入催单记录
     * @param orderRemind 催单记录
     */
    @Insert("insert into order_remind(order_id, user_id, create_time) values(#{orderId}, #{userId}, #{createTime})")
    void insert(OrderRemind orderRemind);
}

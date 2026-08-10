package com.zyj.orderservice.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.mapper
 * @Project：yun-shan
 * @name：OrderDetailMapper
 * @Date：13 12月 2025  16:49
 * @Filename：OrderDetailMapper
 */
@Mapper
public interface OrderDetailMapper {
    /**
     * 插入订单明细批量插入
     * @param orderDetailList
     */
    void insert(List<OrderDetail> orderDetailList);

    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getById(Long id);

    @Select("<script>" +
            "select * from order_detail where order_id in " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);

    
}
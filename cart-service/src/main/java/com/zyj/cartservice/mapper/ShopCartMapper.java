package com.zyj.cartservice.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 购物车数据访问层接口
 * 负责购物车相关的数据库CRUD操作
 *
 * @Author：zyj
 * @Package：com.zyj.cartservice.mapper
 * @Project：yun-shan
 */
@Mapper
public interface ShopCartMapper {

    /**
     * 根据购物车ID更新商品数量
     *
     * @param cart1 购物车对象（需包含id、number字段）
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart1);

    /**
     * 插入单条购物车记录
     *
     * @param shoppingCart 购物车对象（需包含用户ID、商品信息、数量、金额等字段）
     */
    void insertShopCart(ShoppingCart shoppingCart);

    /**
     * 根据条件查询购物车列表
     * 支持按用户ID、菜品ID、套餐ID、菜品口味等条件筛选
     *
     * @param shoppingCart 查询条件对象（封装用户ID、菜品/套餐ID等）
     * @return 符合条件的购物车列表
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据用户ID清空购物车
     *
     * @param userId 用户ID
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void delete(Long userId);

    @Delete("delete from shopping_cart where user_id = #{userId} and merchant_id = #{merchantId}")
    void deleteByUserIdAndMerchantId(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    /**
     * 根据购物车ID删除单条记录
     *
     * @param id 购物车记录ID
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 批量插入购物车记录
     * 用于「再来一单」等批量添加场景
     *
     * @param cart 购物车记录列表
     */
    void insertBatch(List<ShoppingCart> cart);
}
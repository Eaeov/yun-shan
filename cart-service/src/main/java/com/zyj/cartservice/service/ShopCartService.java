package com.zyj.cartservice.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.cartservice.service
 * @Project：yun-shan
 * @name：ShopCartService
 * @Date：11 12月 2025  15:23
 * @Filename：ShopCartService
 */
public interface ShopCartService {

    /**
     * 减少购物车中商品数量
     * @param shoppingCartDTO
     */
    void sub(ShoppingCartDTO shoppingCartDTO);

    /**
     * 添加商品到购物车
     * @param shoppingCartDTO
     */
    void addShopCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询当前登录用户的购物车列表
     * @return
     */
    List<ShoppingCart> list();

    /**
     * 查询当前登录用户指定商家的购物车列表
     * @param merchantId 商家ID
     * @return
     */
    List<ShoppingCart> list(Long merchantId);

    /**
     * 清空当前登录用户的购物车
     */
    void delete();

    /**
     * 清空当前登录用户指定商家的购物车
     * @param merchantId 商家ID
     */
    void delete(Long merchantId);
}

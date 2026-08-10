package com.zyj.yunapi.client;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.yunapi.client
 * @Project：yun-shan
 * @name：CartClient
 * @Date：10 3月 2026  16:32
 * @Filename：CartClient
 */
@FeignClient("cart-service")
public interface CartClient {
    /**
     * 清空购物车
     * @return
     * 来自 shoppingCart
     */
    @DeleteMapping("/user/shoppingCart/clean")
    Result<String> deleteShoppingCart();

    /**
     * 查询购物车
     * 来自 shoppingCart
     */
    @GetMapping("/user/shoppingCart/list")
    Result<List<ShoppingCart>> listShoppingCart();


    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     * 来自 shoppingCart
     */
    @PostMapping("/user/shoppingCart/add")
    Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO);

}

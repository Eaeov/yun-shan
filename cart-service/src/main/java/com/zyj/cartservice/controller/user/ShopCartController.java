package com.zyj.cartservice.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;

import com.zyj.cartservice.service.ShopCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.cartservice.controller.user
 * @Project：yun-shan
 * @name：ShopCartController
 * @Date：11 12月 2025  15:20
 * @Filename：ShopCartController
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShopCartController {

    private final ShopCartService shopCartService;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车，商品信息为：{}", shoppingCartDTO);
        shopCartService.addShopCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查询购物车
     * @param merchantId 商家ID（可选，传值则查询指定商家购物车）
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(@RequestParam(required = false) Long merchantId) {
        List<ShoppingCart> list;
        if (merchantId != null) {
            log.info("查询指定商家购物车，merchantId={}", merchantId);
            list = shopCartService.list(merchantId);
        } else {
            log.info("查询全部购物车");
            list = shopCartService.list();
        }
        return Result.success(list);
    }

    /**
     * 清空购物车
     * @param merchantId 商家ID（可选，传值则清空指定商家购物车）
     * @return
     */
    @DeleteMapping("/clean")
    public Result<String> delete(@RequestParam(required = false) Long merchantId) {
        if (merchantId != null) {
            log.info("清空指定商家购物车，merchantId={}", merchantId);
            shopCartService.delete(merchantId);
        } else {
            log.info("清空全部购物车");
            shopCartService.delete();
        }
        return Result.success();
    }

    /**
     * 减少购物车中商品数量
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("减少购物车商品数量：{}", shoppingCartDTO);
        shopCartService.sub(shoppingCartDTO);
        return Result.success();
    }
}

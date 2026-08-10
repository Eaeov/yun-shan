package com.zyj.productservice.controller.user;


import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import com.zyj.productservice.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.user
 * @Project：yun-shan
 * @name：DishController
 * @Date：09 12月 2025  21:30
 * @Filename：DishController
 * 用户端-菜品浏览（多商家 + Redis缓存）
 */
@Slf4j
@RestController("UserDishControllerProduct")
@RequiredArgsConstructor
@RequestMapping("/user/dish")
public class DishController {

    private final DishService dishService;

    /**
     * 根据分类id和商家ID查询菜品及口味（用户端，带Redis缓存）
     * key = dishCache::{merchantId}::{categoryId}
     * @param categoryId 分类ID
     * @param merchantId 商家ID（必填）
     * @return
     */
    @GetMapping("/list")
    public Result<List<DishVO>> listByCategoryAndMerchant(
            @RequestParam Long categoryId,
            @RequestParam Long merchantId) {
        log.info("用户端查询菜品，categoryId={}, merchantId={}", categoryId, merchantId);
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setMerchantId(merchantId);
        List<DishVO> dishVOS = dishService.listWithFlavor(dish);
        return Result.success(dishVOS);
    }

    @GetMapping("/search")
    public Result<List<DishVO>> searchDish(@RequestParam String keyword, @RequestParam Long merchantId){
        log.info("用户端搜索菜品，keyword={}, merchantId={}", keyword, merchantId);
        List<DishVO> dishVOS = dishService.searchDish(keyword, merchantId);
        return Result.success(dishVOS);
    }

}

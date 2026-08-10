package com.zyj.productservice.controller.user;


import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import com.zyj.productservice.service.SetmealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.user
 * @Project：yun-shan
 * @name：SetmealController
 * @Date：09 12月 2025  21:33
 * @Filename：SetmealController
 * 用户端-套餐浏览（多商家 + Redis缓存）
 */
@Slf4j
@RestController("UserSetmealControllerProduct")
@RequiredArgsConstructor
@RequestMapping("/user/setmeal")
public class SetmealController {

    private final SetmealService setmealService;

    /**
     * 查询套餐列表（用户端，带Redis缓存）
     * key = setmealCache::{merchantId}::{categoryId}
     * @param categoryId 分类ID
     * @param merchantId 商家ID（必填）
     * @return
     */
    @GetMapping("/list")
    public Result<List<SetmealVO>> list(
            @RequestParam Long categoryId,
            @RequestParam Long merchantId) {
        log.info("用户端查询套餐列表，categoryId={}, merchantId={}", categoryId, merchantId);
        List<SetmealVO> setmealVOList = setmealService.listWithCache(merchantId, categoryId);
        return Result.success(setmealVOList);
    }

    /**
     * 根据套餐id查询包含的菜品（不需要缓存，数据较动态）
     * @param id 套餐ID
     * @return
     */
    @GetMapping("/dish/{id}")
    public Result<List<DishItemVO>> getDishItemById(@PathVariable Long id) {
        log.info("根据套餐id查询菜品：{}", id);
        List<DishItemVO> dishItemVOList = setmealService.getDishItemById(id);
        return Result.success(dishItemVOList);
    }

    /**
     * 模糊查询套餐
     */
    @GetMapping("/search")
    public Result<List<SetmealVO>> searchSetmeal(
            @RequestParam String name,
            @RequestParam Long categoryId,
            @RequestParam Long merchantId) {
        log.info("模糊查询套餐，name={}, categoryId={}, merchantId={}", name, categoryId, merchantId);
        List<SetmealVO> setmealVOList = setmealService.searchSetmeal(name, categoryId, merchantId);
        return Result.success(setmealVOList);
    }
}

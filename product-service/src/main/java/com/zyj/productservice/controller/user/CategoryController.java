package com.zyj.productservice.controller.user;


import com.sky.entity.Category;
import com.sky.result.Result;
import com.zyj.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.user
 * @Project：yun-shan
 * @name：CategoryController
 * @Date：19 3 月 2026  14:54
 * @Filename：CategoryController
 * 用户端-分类浏览（多商家 + Redis缓存）
 */
@Slf4j
@RestController("UserCategoryControllerProduct")
@RequiredArgsConstructor
@RequestMapping("/user/category")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询分类列表（用户端，带Redis缓存）
     * key = categoryCache::{merchantId}::{type}
     * @param type 分类类型
     * @param merchantId 商家ID（必填）
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam Long merchantId) {
        log.info("用户端查询分类列表，type={}, merchantId={}", type, merchantId);
        List<Category> categories = categoryService.listWithCache(merchantId, type);
        return Result.success(categories);
    }
}

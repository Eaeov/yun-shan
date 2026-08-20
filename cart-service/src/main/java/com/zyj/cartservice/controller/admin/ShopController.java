package com.zyj.cartservice.controller.admin;

import com.sky.constant.RedisStatusConstant;
import com.sky.result.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * @Author：zyj
 * @Package：com.zyj.cartservice.controller.admin
 * @Project：yun-shan
 * @name：ShopController
 * @Date：08 12月 2025  21:03
 * @Filename：ShopController
 */
@RestController("adminShopController")
@RequestMapping("/admin/shop")

@RequiredArgsConstructor
@Slf4j
public class ShopController {

    // 改为：使用前缀 + 商家ID拼接

    private final RedisTemplate redisTemplate ;

    /**
     * 设置店铺状态
     * @param merchantId 商家ID
     * @param status 1:营业中 0:打烊中
     */
    @PutMapping("/{merchantId}/{status}")
    public Result<String> setStatus(@PathVariable Long merchantId,
                                    @PathVariable Integer status) {
        log.info("设置商家[{}]店铺状态：{}", merchantId, status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(RedisStatusConstant.SHOP_STATUS_PREFIX + merchantId, status, 24, TimeUnit.HOURS);
        return Result.success();
    }


    /**
     * 获得店铺状态
     * @param merchantId 商家ID
     */
    @GetMapping("/{merchantId}/status")
    public Result<Integer> getStatus(@PathVariable Long merchantId) {
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisStatusConstant.SHOP_STATUS_PREFIX + merchantId);
        log.info("获得商家[{}]店铺状态：{}", merchantId, status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}

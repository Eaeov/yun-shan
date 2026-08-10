package com.zyj.cartservice.controller.user;

import com.sky.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author：zyj
 * @Package：com.zyj.cartservice.controller.user
 * @Project：yun-shan
 * @name：ShopController
 * @Date：08 12月 2025  21:03
 * @Filename：ShopController
 */
@RequiredArgsConstructor
@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
public class ShopController {

    public static final String SHOP_STATUS_PREFIX = "shop:status:";

    private final RedisTemplate redisTemplate;

    /*
     * 获得店铺状态
     */
    @GetMapping("/{merchantId}/status")
    public Result<Integer> getStatus(@PathVariable Long merchantId){
        Integer status =(Integer) redisTemplate.opsForValue().get(SHOP_STATUS_PREFIX + merchantId);
        if (status == null) {
            status = 0;
        }
        log.info("获得商家[{}]店铺状态：{}", merchantId, status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }


}

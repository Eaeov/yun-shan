package com.zyj.productservice.listener;

import com.zyj.productservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 菜品缓存清除监听器
 * 注意：当前 DishServiceImpl 已改为直接使用 RedisTemplate 清除缓存，
 * 该监听器保留作为兜底方案，处理可能存在的外部消息。
 *
 * @Author：zyj
 * @Package：com.zyj.productservice.listener
 * @Project：yun-shan
 * @name：DishCacheListener
 * @Date：04 4月 2026  17:21
 * @Filename：DishCacheListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DishCacheListener {
    private final RedisTemplate redisTemplate;

    private static final String DISH_CACHE_PREFIX = "dishCache:";

    @RabbitListener(queues = RabbitMQConfig.DISH_CACHE_QUEUE)
    public void clearDishCache(Map<String, Object> message) {
        log.info("收到菜品缓存清除消息: {}", message);

        if (message != null && message.containsKey("categoryId")) {
            Long categoryId = ((Number) message.get("categoryId")).longValue();

            // 兼容旧版 key（无 merchantId）：dist_{categoryId}
            String oldKey = "dist_" + categoryId;
            redisTemplate.delete(oldKey);

            // 新版 key（有 merchantId 时）：dishCache::{merchantId}::{categoryId}
            if (message.containsKey("merchantId")) {
                Long merchantId = ((Number) message.get("merchantId")).longValue();
                String newKey = DISH_CACHE_PREFIX + merchantId + "::" + categoryId;
                Boolean deleted = redisTemplate.delete(newKey);
                log.info("清除多商家菜品缓存，key={}, result={}", newKey, deleted);
            } else {
                log.info("清除旧版菜品缓存，key={}", oldKey);
            }
        } else {
            log.warn("收到的消息格式不正确: {}", message);
        }
    }
}

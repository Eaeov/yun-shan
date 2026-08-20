package com.zyj.productservice.listener;

import com.zyj.productservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 套餐缓存清除监听器（MQ 异步精确删除）
 *
 * 【优化点】
 * 旧版：用 pattern "setmealCache:{merchantId}::*" 批量删除 → 改一个套餐，全商家分类缓存失效
 * 新版：精确删除 "setmealCache::{merchantId}::{categoryId}" 这一个 key → 只影响当前分类
 *
 * 【完整链路】
 * 1. SetmealServiceImpl CRUD 操作 → afterCommit() → sendSetmealCacheClearMessage()
 * 2. RabbitMQ 转发消息到 setmeal.cache.queue
 * 3. 本 Listener 收到消息 → 精确删除单个 Redis key
 *
 * @Author：zyj
 * @Package：com.zyj.productservice.listener
 * @Project：yun-shan
 * @name：SetmealCacheListener
 * @Date：20 8月 2026
 * @Filename：SetmealCacheListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SetmealCacheListener {

    private final RedisTemplate redisTemplate;

    private static final String SETMEAL_CACHE_PREFIX = "setmealCache:";

    /**
     * 监听套餐缓存清除消息（精确到 categoryId）
     * 消息格式：{ "merchantId": 123, "categoryId": 456 }
     */
    @RabbitListener(queues = RabbitMQConfig.SETMEAL_CACHE_QUEUE)
    public void clearSetmealCache(Map<String, Object> message) {
        log.info("【MQ】收到套餐缓存清除消息: {}", message);

        if (message == null || !message.containsKey("merchantId")) {
            log.warn("【MQ】收到的套餐缓存清除消息格式不正确: {}", message);
            return;
        }

        try {
            Long merchantId = ((Number) message.get("merchantId")).longValue();
            Long categoryId = message.containsKey("categoryId")
                ? ((Number) message.get("categoryId")).longValue()
                : null;

            // 【核心优化】精确删除单个 key，而不是用 pattern 批量删
            String key = SETMEAL_CACHE_PREFIX + merchantId + "::" + categoryId;
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("【MQ】已精确删除套餐缓存，key={}, merchantId={}, categoryId={}",
                    key, merchantId, categoryId);
            } else {
                log.info("【MQ】套餐缓存 key 不存在（可能已过期），key={}", key);
            }
        } catch (Exception e) {
            log.error("【MQ】处理套餐缓存清除消息失败", e);
        }
    }
}
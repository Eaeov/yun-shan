package com.zyj.productservice.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.config
 * @Project：yun-shan
 * @name：RabbitMQConfig
 * @Date：04 4月 2026  15:40
 * @Filename：RabbitMQConfig
 */
@Configuration
public class RabbitMQConfig {
    // ==================== 菜品缓存 MQ ====================
    public static final String DISH_CACHE_EXCHANGE = "dish.cache.exchange";
    public static final String DISH_CACHE_QUEUE = "dish.cache.queue";
    public static final String DISH_CLEAR_KEY = "dish.clear";

    // ==================== 套餐缓存（新增，学习用） ====================
    public static final String SETMEAL_CACHE_EXCHANGE = "setmeal.cache.exchange";
    public static final String SETMEAL_CACHE_QUEUE = "setmeal.cache.queue";
    public static final String SETMEAL_CLEAR_KEY = "setmeal.clear";

    /**
     * 创建菜品缓存 Direct 交换机（精确路由，简单高效）
     */
    @Bean
    public Exchange dishCacheExchange() {
        return ExchangeBuilder.directExchange(DISH_CACHE_EXCHANGE).durable(true).build();
    }

    /**
     * 创建菜品缓存队列
     */
    @Bean
    public Queue dishCacheQueue() {
        return QueueBuilder.durable(DISH_CACHE_QUEUE).build();
    }

    /**
     * 绑定菜品缓存队列到交换机
     */
    @Bean
    public Binding dishCacheBinding(Queue dishCacheQueue, Exchange dishCacheExchange) {
        return BindingBuilder.bind(dishCacheQueue)
                .to(dishCacheExchange)
                .with(DISH_CLEAR_KEY)
                .noargs();
    }

    /**
     * 配置消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== 套餐缓存 MQ 配置 ====================

    /**
     * 创建套餐缓存 Direct 交换机（学习用：Direct 比 Topic 更简单直观）
     */
    @Bean
    public Exchange setmealCacheExchange() {
        return ExchangeBuilder.directExchange(SETMEAL_CACHE_EXCHANGE).durable(true).build();
    }

    /**
     * 创建套餐缓存队列
     */
    @Bean
    public Queue setmealCacheQueue() {
        return QueueBuilder.durable(SETMEAL_CACHE_QUEUE).build();
    }

    /**
     * 绑定套餐缓存队列到交换机
     */
    @Bean
    public Binding setmealCacheBinding(Queue setmealCacheQueue, Exchange setmealCacheExchange) {
        return BindingBuilder.bind(setmealCacheQueue)
                .to(setmealCacheExchange)
                .with(SETMEAL_CLEAR_KEY)
                .noargs();
    }
}
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
    // 交换机名称
    public static final String DISH_CACHE_EXCHANGE = "dish.cache.exchange";

    // 队列名称
    public static final String DISH_CACHE_QUEUE = "dish.cache.queue";

    // 路由键前缀
    public static final String DISH_UPDATE_KEY = "dish.update.*";
    public static final String DISH_DELETE_KEY = "dish.delete.*";

    /**
     * 创建 Topic 交换机
     */
    @Bean
    public Exchange dishCacheExchange() {
        return ExchangeBuilder.topicExchange(DISH_CACHE_EXCHANGE).durable(true).build();
    }

    /**
     * 创建队列
     */
    @Bean
    public Queue dishCacheQueue() {
        return QueueBuilder.durable(DISH_CACHE_QUEUE).build();
    }

    /**
     * 绑定队列到交换机（支持更新和删除两种路由键）
     */
    @Bean
    public Binding dishUpdateBinding(Queue dishCacheQueue, Exchange dishCacheExchange) {
        return BindingBuilder.bind(dishCacheQueue)
                .to(dishCacheExchange)
                .with(DISH_UPDATE_KEY)
                .noargs();
    }

    /**
     * 绑定队列到交换机（支持删除一种路由键）
     */
    @Bean
    public Binding dishDeleteBinding(Queue dishCacheQueue, Exchange dishCacheExchange) {
        return BindingBuilder.bind(dishCacheQueue)
                .to(dishCacheExchange)
                .with(DISH_DELETE_KEY)
                .noargs();
    }

    /**
     * 配置消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}


package com.sky.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 统一 Redis 配置（下沉至 yun-common，解决跨服务序列化不一致问题）
 * 所有服务共用同一套 RedisTemplate 配置，确保 key 序列化方式一致
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.database}")
    private int database;

    /**
     * RedissonClient：仅供引入 redisson-spring-boot-starter 的服务（product-service）使用。
     * @ConditionalOnMissingBean 避免与 starter 自带的自动配置重复注册导致 bean 冲突。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnClass(Redisson.class)
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setDatabase(database);
        return Redisson.create(config);
    }
}

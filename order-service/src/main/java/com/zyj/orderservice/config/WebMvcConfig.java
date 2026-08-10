package com.zyj.orderservice.config;

import com.zyj.orderservice.interceptor.OrderServiceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.config
 * @Project：yun-shan
 * @name：WebMvcConfig
 * @Date：20 3 月 2026
 * @Filename：WebMvcConfig
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final OrderServiceInterceptor orderServiceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orderServiceInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/admin/**"); // 排除网关密钥验证
    }
}

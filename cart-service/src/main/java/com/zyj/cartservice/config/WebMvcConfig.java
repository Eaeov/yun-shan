package com.zyj.cartservice.config;

import com.zyj.cartservice.interceptors.CartServiceInterceptors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author：zyj
 * @Package：com.zyj.cartservice.config
 * @Project：yun-shan
 * @name：WebMvcConfig
 * @Date：20 3月 2026  11:23
 * @Filename：M
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CartServiceInterceptors cartServiceInterceptors;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cartServiceInterceptors)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/admin/**");
    }
}
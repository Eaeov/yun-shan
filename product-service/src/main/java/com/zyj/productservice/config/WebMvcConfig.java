package com.zyj.productservice.config;

import com.zyj.productservice.interceptor.ProductServiceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.config
 * @Project：yun-shan
 * @name：WebMvcConfig
 * @Date：20 3 月 2026
 * @Filename：WebMvcConfig
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final ProductServiceInterceptor productServiceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(productServiceInterceptor)
                .addPathPatterns("/user/**") // 拦截所有以/user/开头的请求
                .excludePathPatterns("/admin/**"); // 排除以/admin/开头的请求
    }
}

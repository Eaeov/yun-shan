package com.sky.config;

import com.sky.interceptors.UserInfoInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author：zyj
 * @Package：com.sky.config
 * @Project：yun-shan
 * @name：MvcConfig
 * @Date：19 3月 2026  19:18
 * @Filename：MvcConfig
 */
@Configuration
@ConditionalOnClass(DispatcherServlet.class)// 作用是判断DispatcherServlet类是否存在，如果存在则加载该配置类
// （网关中没有MVC依赖启动会报错所以排除DispatcherServlet.class来避免启动报错）
public class MvcConfig implements WebMvcConfigurer {
    /**
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加用户信息拦截器
        registry.addInterceptor(new UserInfoInterceptor());
    }
}

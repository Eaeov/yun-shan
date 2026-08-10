package com.zyj.yunapi.config;

import com.sky.constant.MessageConstant;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author：zyj
 * @Package：com.zyj.yunapi.config
 * @Project：yun-shan
 * @name：FeignConfig
 * @Date：2026-03-10
 * @Filename：FeignConfig
 * @Description: OpenFeign 配置类
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * 配置 Feign 日志级别
     * @return Logger.Level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }


    /**
     * Feign 拦截器，用于传递 JWT Token、用户信息、内部调用标识
     * 内部调用标识 X-Internal-Call 用于让下游服务识别这是服务间调用，跳过网关密钥校验
     * 瑞快斯特 因特赛普特。RequestInterceptor
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 标记为内部服务间调用，下游服务据此跳过网关密钥校验
                template.header(MessageConstant.INTERNAL_CALL_HEADER, MessageConstant.INTERNAL_CALL_VALUE);

                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null) {
                    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

                    // 1. 传递 JWT Token（兼容多种 Header 名称）
                    String userToken = request.getHeader("Authentication");
                    if (userToken == null || userToken.isEmpty()) {
                        userToken = request.getHeader("token");
                    }
                    if (userToken != null && !userToken.isEmpty()) {
                        template.header("Authentication", userToken);
                        log.debug("Feign 传递用户 Token: {}", userToken);
                    }

                    // 2. 传递用户 ID（从 user_info 头获取）
                    String userId = request.getHeader(MessageConstant.USER_INFO);
                    if (userId != null && !userId.isEmpty()) {
                        template.header(MessageConstant.USER_INFO, userId);
                        log.debug("Feign 传递用户 ID: {}", userId);
                    }

                    // 3. 传递网关密钥（仅当原始请求携带时才传递，不做默认补全）
                    String gatewayKey = request.getHeader(MessageConstant.GATEWAY_SECRET_KEY);
                    if (gatewayKey != null && !gatewayKey.isEmpty()) {
                        template.header(MessageConstant.GATEWAY_SECRET_KEY, gatewayKey);
                        log.debug("Feign 传递网关密钥：{}", gatewayKey);
                    }

                    // 4. 传递管理端商家ID和角色（多租户上下文）
                    String merchantId = request.getHeader("emp-merchant-id");
                    if (merchantId != null && !merchantId.isEmpty()) {
                        template.header("emp-merchant-id", merchantId);
                        log.debug("Feign 传递商家ID：{}", merchantId);
                    }
                    String role = request.getHeader("emp-role");
                    if (role != null && !role.isEmpty()) {
                        template.header("emp-role", role);
                        log.debug("Feign 传递角色：{}", role);
                    }
                }
            }
        };
    }
}
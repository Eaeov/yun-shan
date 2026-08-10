package com.zyj.orderservice.interceptor;

import cn.hutool.core.util.StrUtil;
import com.sky.config.GatewayConfig;
import com.sky.constant.MessageConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 订单服务拦截器：网关密钥验证
 * 网关已验证JWT，微服务只需验证网关密钥即可放行
 * 用户/员工信息由yun-common的UserInfoInterceptor统一设置
 *
 * 校验逻辑：
 * 1. X-Internal-Call: true → 内部 Feign 调用，跳过网关密钥校验
 * 2. gatewaySecretKey 匹配 → 网关转发，校验通过
 * 3. 都没有 → 绕过网关的直连调用，拒绝
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderServiceInterceptor implements HandlerInterceptor {

    private final GatewayConfig gatewayConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 内部服务间 Feign 调用，跳过网关密钥校验
        if (MessageConstant.INTERNAL_CALL_VALUE.equals(request.getHeader(MessageConstant.INTERNAL_CALL_HEADER))) {
            log.debug("订单服务：内部调用，跳过网关密钥校验");
            return true;
        }

        String userId = request.getHeader(MessageConstant.USER_INFO);
        if (!validateUserId(userId, response)) {
            return false;
        }

        String gatewayKey = request.getHeader(MessageConstant.GATEWAY_SECRET_KEY);
        return validateGatewayKey(gatewayKey, response);
    }

    /**
     * 验证用户 ID
     */
    private boolean validateUserId(String userId, HttpServletResponse response) {
        if (StrUtil.isBlank(userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("订单服务拦截：用户未登录");
            return false;
        }
        return true;
    }

    /**
     * 验证网关密钥
     */
    private boolean validateGatewayKey(String gatewayKey, HttpServletResponse response) {
        if (StrUtil.isBlank(gatewayKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("订单服务拦截：网关密钥为空");
            return false;
        }

        if (!gatewayConfig.getSecretKey().equals(gatewayKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("订单服务拦截：网关密钥验证失败");
            return false;
        }
        return true;
    }

}
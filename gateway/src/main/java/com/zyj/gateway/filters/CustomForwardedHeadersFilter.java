package com.zyj.gateway.filters;

import com.sky.config.GatewayConfig;
import com.sky.constant.MessageConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Author：zyj
 * @Package：com.zyj.gateway.filters
 * @Project：yun-shan
 * @name：CustomForwardedHeadersFilter
 * @Date：12 3月 2026  17:05
 * @Filename：CustomForwardedHeadersFilter
 */

import com.sky.constant.JwtClaimsConstant;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;


import java.util.List;

/**
 * JWT 令牌校验的全局过滤器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomForwardedHeadersFilter implements GlobalFilter, Ordered {//数字越小优先级越高

    private final JwtProperties jwtProperties;
    private final GatewayConfig gatewayConfig;

    // 路径匹配器
    private PathMatcher pathMatcher = new AntPathMatcher();

    // 不需要拦截的白名单路径
    private static final List<String> WHITELIST = List.of(
            //"/**",//测试全部放行
            "/user/user/login",      // 用户登录
            "/admin/employee/login",  // 员工登录
            "/user/shop/status",       // 店铺营业状态
             "/ws/**",                  // WebSocket（通过查询参数传 token，@OnOpen 中自行校验）
            "/api/ai/stream"           // AI接口
    );

    /**
     * JWT 令牌校验的过滤器
     * @param exchange
     * @param chain
     * @return Mono<Void>
     * 实现流程： 1.获取request对象
     *          2.检查是否是白名单路径，如果是则直接放行（是否需要拦截）
     *          3.从请求头中获取 token
     *          4.校验 token
     *          5.将用户 ID 添加到请求头，传递给下游服务
     *          6.继续执行过滤器链
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {//exchange 是请求对象，chain 是过滤器链
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        // 1. 检查是否是白名单路径，如果是则直接放行
        if (isWhitelist(path)) {
            log.info("白名单路径，直接放行：{}", path);
            return chain.filter(exchange);
        }

        // 2. 从请求头中获取 token
        String token = request.getHeaders().getFirst(jwtProperties.getUserTokenName());
        if (token == null || token.isEmpty()) {
            // 尝试从 admin token 头获取（管理端）
            token = request.getHeaders().getFirst(jwtProperties.getAdminTokenName());
        }

        // 3. 校验 token
        if (token == null || token.isEmpty()) {
            log.warn("未携带 token，拒绝访问：{}", path);
            // 返回未授权状态码 返回 401 未授权
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();  //完成响应，立即发送给客户端
            //立即结束，不执行 chain.filter()
        }

        try {
            log.info("JWT 校验开始，path: {}, token: {}", path, token);

            // 尝试用用户密钥解析（先试用户端）
            Claims claims = null; // JWT 声明对象
            Long userId = null;  // 用户ID
            //网关密钥
            String gatewaySecretKey = null;

            try {
                claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                gatewaySecretKey = gatewayConfig.getSecretKey();
                userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                log.info("用户端 JWT 校验成功，userId: {}", userId);
            } catch (Exception e) {
                // 用户密钥解析失败，尝试用管理员密钥解析
                try {
                    claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                    gatewaySecretKey = gatewayConfig.getSecretKey();
                    userId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
                    log.info("管理端 JWT 校验成功，empId: {}", userId);
                } catch (Exception e2) {
                    log.error("JWT 校验失败：{}", e2.getMessage());
                    exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED); // 设置状态码为未授权
                    return exchange.getResponse().setComplete();
                }
            }

            // 4. 将用户信息添加到请求头，传递给下游服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(MessageConstant.USER_INFO, userId.toString())
                    .header(MessageConstant.GATEWAY_SECRET_KEY, gatewaySecretKey)
                    .build();

            // 管理端JWT额外传递商家ID和角色，供下游服务做权限判断
            Object merchantIdObj = claims.get(JwtClaimsConstant.EMP_MERCHANT_ID);
            Object roleObj = claims.get(JwtClaimsConstant.EMP_ROLE);
            if (merchantIdObj != null) {
                mutatedRequest = mutatedRequest.mutate()
                        .header("emp-merchant-id", merchantIdObj.toString())
                        .build();
            }
            if (roleObj != null) {
                mutatedRequest = mutatedRequest.mutate()
                        .header("emp-role", roleObj.toString())
                        .build();
            }

            log.info("JWT 校验通过，转发请求到下游服务，userId: {}, isAdmin: {}", userId);

            // 5. 继续执行过滤器链
            return chain.filter(exchange.mutate().request(mutatedRequest).build()); // 转发请求到下游服务，把mutatedRequest代替原来的request

        } catch (Exception ex) {
            log.error("JWT 处理异常：{}", ex.getMessage(), ex);
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 判断是否是白名单路径
     */
    private boolean isWhitelist(String path) {//path是请求的路径
        for (String whitelistPath : WHITELIST) {//WHITELIST 是白名单路径列表
            if (pathMatcher.match(whitelistPath, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置过滤器的优先级，数字越小优先级越高
     * JWT 校验应该在其他过滤器之前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
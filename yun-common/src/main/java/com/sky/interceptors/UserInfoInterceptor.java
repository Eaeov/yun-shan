package com.sky.interceptors;

import cn.hutool.core.util.StrUtil;
import com.sky.constant.MessageConstant;
import com.sky.context.AuthContext;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一用户信息拦截器（yun-common公共模块）
 * 从网关传递的Header中解析用户/员工信息，同时设置BaseContext和AuthContext
 * 下游微服务无需重复处理，只需验证网关密钥即可
 */
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userInfo = request.getHeader(MessageConstant.USER_INFO); // 从Header中获取用户ID
        if (StrUtil.isBlank(userInfo)) {
            return true;
        }

        Long id = Long.valueOf(userInfo);
        BaseContext.setCurrentId(id);

        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/")) {
            String merchantIdStr = request.getHeader("emp-merchant-id");
            String roleStr = request.getHeader("emp-role");

            Long merchantId = StrUtil.isNotBlank(merchantIdStr) ? Long.valueOf(merchantIdStr) : null;
            Integer role = StrUtil.isNotBlank(roleStr) ? Integer.valueOf(roleStr) : null;

            Employee employee = Employee.builder()
                    .id(id)
                    .merchantId(merchantId)
                    .role(role)
                    .build();

            AuthContext.setCurrentEmployee(employee);
        } else {
            AuthContext.setCurrentUserId(id);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
        AuthContext.removeCurrentEmployee();
        AuthContext.removeCurrentUserId();
    }
}

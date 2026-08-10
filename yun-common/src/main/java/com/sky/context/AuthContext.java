package com.sky.context;

import com.sky.entity.Employee;

/**
 * 权限上下文工具类
 * 从ThreadLocal中获取当前登录的员工信息或用户ID
 */
public class AuthContext {

    private static final ThreadLocal<Employee> employeeThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前登录员工信息
     * @param employee 员工对象
     */
    public static void setCurrentEmployee(Employee employee) {
        employeeThreadLocal.set(employee);
    }

    /**
     * 获取当前登录员工信息
     * @return 员工对象（含 id, merchantId, role）
     */
    public static Employee getCurrentEmployee() {
        return employeeThreadLocal.get();
    }

    /**
     * 移除当前登录员工信息
     */
    public static void removeCurrentEmployee() {
        employeeThreadLocal.remove();
    }

    /**
     * 设置当前登录用户ID（C端用户）
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Long userId) {
        userIdThreadLocal.set(userId);
    }

    /**
     * 获取当前登录用户ID（C端用户）
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return userIdThreadLocal.get();
    }

    /**
     * 移除当前登录用户ID
     */
    public static void removeCurrentUserId() {
        userIdThreadLocal.remove();
    }

    /**
     * 判断当前员工是否为超级管理员
     * @return true=超管
     */
    public static boolean isSuperAdmin() {
        Employee employee = getCurrentEmployee();
        return employee != null && employee.getRole() != null && employee.getRole() == 0;
    }

    /**
     * 获取当前员工的商家ID
     * @return 商家ID，超管返回null
     */
    public static Long getCurrentMerchantId() {
        Employee employee = getCurrentEmployee();
        return employee != null ? employee.getMerchantId() : null;
    }
}

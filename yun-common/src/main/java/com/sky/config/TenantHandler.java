package com.sky.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import com.sky.context.AuthContext;
import java.util.Set;

/**
 * 多租户处理器工厂
 * <p>
 * 通过 MyBatis-Plus TenantLineInnerInterceptor 自动为 SQL 追加 merchant_id 条件，
 * 替代各 Service 中手工拼接的 wrapper.eq("merchant_id", ...) 代码。
 * </p>
 *
 * <h3>租户ID获取逻辑</h3>
 * <ul>
 *   <li>普通员工 → 返回其所属商家的 merchantId → SQL 自动追加 AND merchant_id = ?</li>
 *   <li>超级管理员 → 返回 null → 跳过租户过滤 → 可查看所有商家</li>
 *   <li>C端用户 → 返回 null（无 Employee 上下文）→ 跳过租户过滤</li>
 * </ul>
 *
 * <h3>白名单机制</h3>
 * 只有 TENANT_TABLES 中列出的表才会追加 tenant_id 条件，其他表不受影响。
 *
 * @author zyj
 * @date 2026-07-01
 */
public final class TenantHandler {

    /**
     * 需要租户隔离的数据库表（白名单）
     * 不在白名单中的表不会追加 merchant_id 条件
     */
    private static final Set<String> TENANT_TABLES = Set.of(
            "dish",
            "setmeal",
            "category",
            "orders",
            "shopping_cart",
            "employee"
    );

    private TenantHandler() {
        // 工具类禁止实例化
    }

    /**
     * 创建 TenantLineInnerInterceptor 实例
     * 各服务的 MybatisConfig 中调用此方法注册拦截器
     */
    public static TenantLineInnerInterceptor create() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {

            @Override
            public Expression getTenantId() {
                Long merchantId = AuthContext.getCurrentMerchantId();
                // 超管 / C端用户 / 未登录 → 返回 null 真正跳过租户条件
                // 注意：不能返回 new NullValue()，否则 MyBatis-Plus 会拼接 AND merchant_id = NULL，
                // 而 SQL 中 = NULL 永远为 UNKNOWN，导致查不到任何数据
                if (merchantId == null) {
                    return null;
                }
                return new LongValue(merchantId);  // 追加 AND merchant_id = ?
            }

            @Override
            public String getTenantIdColumn() {
                return "merchant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 白名单模式：只对 TENANT_TABLES 中的表追加租户条件
                return !TENANT_TABLES.contains(tableName);
            }
        });
    }
}
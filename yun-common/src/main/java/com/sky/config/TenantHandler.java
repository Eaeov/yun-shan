package com.sky.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import com.sky.context.AuthContext;
import org.apache.http.client.AuthCache;

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
    private static boolean shouldIgnoreTable(String tableName) {
        if(!TENANT_TABLES.contains(tableName)) return true;
        return AuthContext.getCurrentMerchantId() == null;
    }
    /**
     * 创建 TenantLineInnerInterceptor 实例
     * 各服务的 MybatisConfig 中调用此方法注册拦截器
     * 顺序 ignoreTable →（放行了才轮到）getTenantIdColumn → getTenantId
     */
    public static TenantLineInnerInterceptor create() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {

            // 获得要追加的值
            @Override
            public Expression getTenantId() {
                Long merchantId = AuthContext.getCurrentMerchantId();
                // 防御性检查：shouldIgnoreTable 已过滤 null 场景
                if (merchantId == null) {
                    return null;
                }
                return new LongValue(merchantId);
            }

            // 自定义租户ID列名
            @Override
            public String getTenantIdColumn() {
                return "merchant_id";
            }
            // 先对每张白名单表判断是否需要跳过 然后再走getTenantIdColumn和getTenantId()
            @Override
            public boolean ignoreTable(String tableName) {
                // 白名单模式：只对 TENANT_TABLES 中的表追加租户条件
                return shouldIgnoreTable(tableName);
            }
        });
    }
}
/**
 * 你调用 dishMapper.selectList()
 *         │
 *         ▼
 * ① 拦截器接管，取出 SQL 字符串："SELECT * FROM dish"
 *         │
 *         ▼
 * ② 用 JSqlParser 把 SQL 解析成一棵"语法树"，找出里面出现的所有表
 *    → 发现 1 张表：dish
 *         │
 *         ▼
 * ③ 对每张表，先问一次：ignoreTable("dish")
 *    → 走你的 shouldIgnoreTable：
 *       dish ∈ 白名单? 是 → 再问 AuthContext.getCurrentMerchantId()
 *       → 当前员工 merchantId = 1001（不是 null）
 *    → 返回 false  ← 意思是"这张表不要跳过"
 *         │
 *         ▼
 * ④ 既然不跳过，就问你两个信息：
 *    getTenantIdColumn() → "merchant_id"        （列名）
 *    getTenantId()       → new LongValue(1001)  （值）
 *         │
 *         ▼
 * ⑤ 拼成条件：merchant_id = 1001
 *    挂到原 WHERE 后面（没有 WHERE 就新建）：
 *    SELECT * FROM dish WHERE merchant_id = 1001
 *         │
 *         ▼
 * ⑥ 把改好的 SQL 真正发给数据库
 */
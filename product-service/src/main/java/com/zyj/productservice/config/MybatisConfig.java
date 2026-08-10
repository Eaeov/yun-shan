package com.zyj.productservice.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.sky.config.TenantHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.config
 * @Project：yun-shan
 * @name：MybatisConfig
 * @Date：10 12月 2025  20:53
 * @Filename：MybatisConfig
 */
@Configuration
@MapperScan("com.zyj.productservice.mapper")
public class MybatisConfig {


    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        // 初始化MP核心插件容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页插件（核心）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        // 指定数据库类型（MySQL/Oracle/PostgreSQL等），适配不同分页语法
        paginationInterceptor.setDbType(DbType.MYSQL);
        // 可选：分页溢出处理（true=超出页码自动查最后一页，false=返回空）
        paginationInterceptor.setOverflow(true);
        // 可选：限制单页最大条数（防止恶意查10万条，默认无限制）
        paginationInterceptor.setMaxLimit(1000L);
        // 添加分页插件到容器
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 2. 多租户插件 — 自动追加 merchant_id 条件（替代人工拼接）
        TenantLineInnerInterceptor tenantInterceptor = TenantHandler.create();
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 下面可追加其他插件（示例：乐观锁、防全表操作等）
        // interceptor.addInnerInterceptor(乐观锁插件/防全表插件...);

        return interceptor;
    }
}

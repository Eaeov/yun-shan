package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    private String adminSecretKey; // 密钥
    private long adminTtl; // 有效期
    private String adminTokenName; // 令牌名称

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    private String userSecretKey; // 密钥
    private long userTtl; // 有效期
    private String userTokenName; // 令牌名称

}

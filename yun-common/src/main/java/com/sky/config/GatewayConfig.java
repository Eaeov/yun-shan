package com.sky.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GatewayConfig {

    @Value("${sky.gateway.secret-key}")
    private String secretKey;

}
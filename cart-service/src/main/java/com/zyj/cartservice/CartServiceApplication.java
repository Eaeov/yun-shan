package com.zyj.cartservice;

import com.zyj.yunapi.client.ProductClient;
import com.zyj.yunapi.config.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients(
        basePackages = "com.zyj.yunapi.client",
        clients = {ProductClient.class},
        defaultConfiguration = FeignConfig.class // 指定默认配置类
)
@ComponentScan(basePackages = {"com.zyj.cartservice", "com.zyj", "com.sky", "com.zyj.yunapi"})
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }

}

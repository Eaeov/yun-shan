package com.zyj.orderservice;

import com.zyj.yunapi.client.CartClient;
import com.zyj.yunapi.client.ProductClient;
import com.zyj.yunapi.client.UserClient;
import com.zyj.yunapi.config.FeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.zyj.orderservice", "com.sky", "com.zyj.yunapi"})
@MapperScan("com.zyj.orderservice.mapper")
@EnableFeignClients(
        basePackages = "com.zyj.yunapi.client",
        clients = {UserClient.class, CartClient.class, ProductClient.class},
        defaultConfiguration = FeignConfig.class // 指定默认配置类
)
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}

package com.zyj.userservice;

import com.zyj.yunapi.client.OrderClient;
import com.zyj.yunapi.client.ProductClient;
import com.zyj.yunapi.config.FeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.zyj.userservice", "com.sky", "com.zyj.yunapi"})
@MapperScan("com.zyj.userservice.mapper")
@EnableFeignClients(
        basePackages = "com.zyj.yunapi.client",
        clients = {OrderClient.class, ProductClient.class},
        defaultConfiguration = FeignConfig.class // 指定默认配置类
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}

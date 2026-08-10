package com.zyj.userservice.config;

import com.sky.json.JacksonObjectMapper;
import com.zyj.userservice.interceptor.GatewayKeyInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    // 注入新的网关拦截器
    private final GatewayKeyInterceptor gatewayKeyInterceptor;

    /**
     * 注册自定义拦截器（移除JWT，仅保留网关验证）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册【网关密钥验证】拦截器...");

        registry.addInterceptor(gatewayKeyInterceptor)
                .addPathPatterns("/admin/**", "/user/**")
                .excludePathPatterns(
                        "/admin/employee/login",
                        "/user/user/login",
                        "/user/shop/status"
                );
    }
    /**
     * 配置 OpenAPI 3 文档（管理端）
     */
    @Bean
    @Primary
    public OpenAPI adminOpenAPI() {
        log.info("准备生成管理端接口文档...");
        return new OpenAPI()
                .info(new Info()
                        .title("苍穹外卖项目接口文档")
                        .version("2.0")
                        .description("苍穹外卖项目管理端接口"));
    }

    /**
     * 配置 OpenAPI 3 文档（用户端）
     */
    @Bean
    public OpenAPI userOpenAPI() {
        log.info("准备生成用户端接口文档...");
        return new OpenAPI()
                .info(new Info()
                        .title("苍穹外卖项目接口文档")
                        .version("2.0")
                        .description("苍穹外卖项目用户端接口"));
    }

    /**
     * 设置静态资源映射，主要是访问接口文档（html、js、css）
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 扩展Spring MVC框架的消息转化器
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要为消息转换器设置一个对象转换器，对象转换器可以将Java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转化器加入容器中
        converters.add(0,converter);
    }
}

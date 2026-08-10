# OpenFeign 远程调用学习指南

## 📖 什么是 OpenFeign？

OpenFeign 是一个声明式的 HTTP 客户端，它让微服务之间的远程调用变得像本地方法调用一样简单。

## 🏗️ 项目架构

```
yun-shan/
├── yun-api/              # 统一管理 Feign Client 接口
│   └── client/
│       ├── ProductServiceClient.java  # 产品服务客户端
│       └── OrderServiceClient.java   # 订单服务客户端
├── cart-service/         # 购物车服务（调用方）
├── product-service/      # 产品服务（被调用方）
└── order-service/        # 订单服务
```

## 🔧 使用步骤

### 第一步：在 yun-api 中定义 Feign Client 接口

```java
@FeignClient(value = "product-service") // 服务名称
public interface ProductServiceClient {
    
    @GetMapping("/product-service/admin/dish/{id}")
   Result<DishVO> getDishById(@PathVariable("id") Long id);
    
    @GetMapping("/product-service/admin/setmeal/{id}")
   Result<SetmealVO> getSetmealById(@PathVariable("id") Long id);
}
```

**关键点：**
- `@FeignClient(value = "服务名")`：指定要调用的微服务名称（与 Nacos 注册的服务名一致）
- `@GetMapping/@PostMapping`：映射到对方服务的 Controller 接口路径
- 返回值使用 `Result<T>`：统一响应格式
- 参数使用 `@PathVariable/@RequestParam`：明确参数传递方式

### 第二步：在服务模块的 pom.xml 中添加依赖

**cart-service/pom.xml:**
```xml
<!-- yun-api(包含 OpenFeign Client) -->
<dependency>
    <groupId>com.zyj</groupId>
    <artifactId>yun-api</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```

### 第三步：在服务模块中注入并使用 Feign Client

**ShopCartServiceImpl.java:**
```java
@Service
@RequiredArgsConstructor
public class ShopCartServiceImpl implements ShopCartService {
    
    // 注入 Feign Client（Spring 会自动创建代理对象）
    private final ProductServiceClient productClient;
    
    @Override
    public void addShopCart(ShoppingCartDTO shoppingCartDTO) {
        // ... 其他代码 ...
        
        // 远程调用 product-service 查询菜品信息
        DishVO dishVO = productClient.getDishById(dishId).getData();
        cart.setName(dishVO.getName());
        cart.setImage(dishVO.getImage());
        cart.setAmount(dishVO.getPrice());
        
        // 远程调用 product-service 查询套餐信息
        SetmealVO setmealVO = productClient.getSetmealById(setmealId).getData();
        cart.setName(setmealVO.getName());
        cart.setImage(setmealVO.getImage());
        cart.setAmount(setmealVO.getPrice());
    }
}
```

## 💡 核心原理

1. **动态代理**：Spring 为 Feign Client 接口创建代理对象
2. **请求构建**：代理对象根据注解信息构建 HTTP 请求
3. **负载均衡**：通过 LoadBalancer 从 Nacos 获取服务实例列表
4. **HTTP 调用**：使用 Ribbon/LoadBalancer 发起 HTTP 请求
5. **响应解析**：将返回的 JSON 解析为 Java 对象

## ⚠️ 常见错误和解决方案

### 错误 1：类型不匹配
```
不兼容的类型。实际为 com.sky.vo.SetmealVO'，需要 'com.sky.entity.Setmeal'
```

**原因：** Controller 返回的是 VO（视图对象），不是 Entity（实体）

**解决：** 使用正确的接收类型
```java
// ❌ 错误
Setmeal setmeal = productClient.getSetmealById(id).getData();

// ✅ 正确
SetmealVO setmealVO = productClient.getSetmealById(id).getData();
```

### 错误 2：Bean 未找到
```
required a bean of type 'xxx.ProductServiceClient' that could not be found
```

**解决：**
1. 确保已添加 `yun-api` 依赖
2. 确保启动类有 `@EnableFeignClients` 配置（Spring Cloud 已自动启用）

### 错误 3：服务调用失败
```
Could not resolve placeholder 'product-service'
```

**解决：**
1. 确保 Nacos 服务已启动并注册
2. 确保 `@FeignClient` 中的服务名与实际注册的服务名一致

## 🎯 最佳实践

1. **统一管理**：所有 Feign Client 放在 `yun-api` 模块中
2. **版本控制**：通过父 pom 管理依赖版本
3. **路径规范**：URL 路径包含服务名，避免路由冲突
4. **异常处理**：添加降级逻辑（后续可学习 Sentinel）
5. **日志配置**：开发环境开启 Feign 调试日志

## 📝 配置文件示例

**application.yml (开启 Feign 日志):**
```yaml
logging:
  level:
    com.zyj.yunapi.client: debug  # 查看 Feign 请求详情
```

## 🚀 下一步学习

1. **Feign 配置优化**：超时时间、重试机制
2. **性能调优**：连接池配置（HttpClient/OkHttp）
3. **容错降级**：集成 Sentinel 实现熔断
4. **请求拦截**：添加 Token 等认证信息
5. **参数校验**：统一的请求参数校验机制

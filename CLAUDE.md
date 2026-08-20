# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概览

**yun-shan（云山）**是一个基于 Spring Cloud Alibaba 的微服务外卖/餐饮平台。Java 17，Spring Boot 3.2.4，Spring Cloud 2023.0.1，Spring Cloud Alibaba 2023.0.1.0。

## 构建与运行

```bash
# 构建所有模块
./mvnw clean install -DskipTests

# 构建单个模块及其依赖
./mvnw clean install -pl user-service -am

# 运行测试（当前项目中没有测试用例）
./mvnw test
```

没有 Docker Compose。服务通过 IDE 或 `./mvnw spring-boot:run -pl <模块名>` 单独启动。运行需要的基础设施：MySQL（数据库 `yun_shan`）、Redis、Nacos（`192.168.100.128:8848`）、RabbitMQ（仅 product-service 需要）。

## 模块架构

项目中存在两个包命名空间：`com.sky`（共享/通用代码）和 `com.zyj`（各服务专属代码）。所有 `@SpringBootApplication` 启动类都同时扫描 `com.zyj` 和 `com.sky`。

### 共享库（非可执行服务）

- **yun-pojo**（`com.sky`）：实体类（带 `@TableName` 注解）、DTO、VO。纯数据对象，无业务逻辑。最基础的模块——不依赖任何内部模块，被所有其他模块依赖。
- **yun-common**（`com.sky`）：Result 统一返回包装、JWT 工具类、BaseContext（ThreadLocal\<Long\> 存 userId）、AuthContext（ThreadLocal\<Employee\>，提供 `isSuperAdmin()` 和 `getCurrentMerchantId()` 方法）、JwtProperties、AliOssUtil、WeChatPayUtil、JacksonObjectMapper、业务异常类、常量（JWT claims、网关密钥 `"220secretKey"`、状态码等）以及共享的 `UserInfoInterceptor`。通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册——导入此模块即自动为所有服务安装 `UserInfoInterceptor`。
- **yun-api**（`com.zyj.yunapi`）：所有 `@FeignClient` 接口和 `FeignConfig`。不是可执行服务（虽然没有 main 类但 POM 中包含 `spring-boot-maven-plugin`）。作为依赖被需要进行服务间调用的模块引入。`FeignConfig` 注册了两个 `RequestInterceptor` bean：一个从当前 HTTP 请求传播 header，另一个在缺少网关密钥时注入默认值。

### 可执行服务

| 服务 | 端口 | 关键依赖 |
|---|---|---|
| **gateway** | 8080 | spring-cloud-starter-gateway, JJWT, yun-common, yun-pojo |
| **user-service** | 8081 | MySQL, MyBatis-Plus, Redis, Knife4j, jbcrypt |
| **cart-service** | 8082 | MySQL, MyBatis-Plus, Redis, Knife4j |
| **order-service** | 8083 | MySQL, MyBatis-Plus, Redis, Knife4j, WebSocket |
| **product-service** | 8084 | MySQL, MyBatis-Plus, Redis, Redisson, RabbitMQ, Knife4j, 阿里云 OSS |

## 请求流程与认证架构

```
客户端 → Gateway (8080) → [JWT 校验 + header 注入] → 下游服务 → [GatewayKeyInterceptor 验证密钥] → Controller
```

1. **网关 JWT 过滤器**（`gateway/.../filters/CustomForwardedHeadersFilter`）：全局过滤器，拦截除白名单路径（`/user/user/login`、`/admin/employee/login`、`/user/shop/status`）外的所有请求。校验 JWT（先用用户端密钥解析，失败则尝试管理端密钥）。向下游请求注入 header：`user_info`（userId）、`gatewaySecretKey`，管理端用户还会附加 `emp-merchant-id` / `emp-role`。

2. **GatewayKeyInterceptor**（每个服务各自实现）：各服务验证 `gatewaySecretKey` header 是否匹配 `MessageConstant.USER_ADMIN_GATEWAY_SECRET_KEY`。缺失或无效则返回 401。

3. **UserInfoInterceptor**（`yun-common`）：公共拦截器，各服务自动注册。读取网关设置的 `user_info` header，填充 `BaseContext`（ThreadLocal\<Long\>）和 `AuthContext`（当前员工或 userId）。在 `afterCompletion` 中清理 ThreadLocal。

4. **服务间调用（Feign）**：`FeignConfig` 通过 `RequestInterceptor` 将 JWT token、user_info 和网关密钥 header 传播到下游服务。

## 路由规范

- `/admin/**` → 管理端/员工端接口
- `/user/**` → 用户端/C 端接口
- 每个服务在这两个路径前缀下同时暴露管理端和用户端 Controller。网关按路径前缀通过 `lb://` 负载均衡 URI 路由到对应服务。

## 关键模式

- **统一返回格式**：所有 Controller 返回 `com.sky.result.Result<T>`（`code=1` 成功，`code=0` 失败）。
- **MyBatis-Plus**：Entity 继承 `BaseMapper<T>`。复杂查询使用 `src/main/resources/mapper/` 下的 XML mapper。
- **AOP 自动填充**：mapper 方法上的 `@AutoFill` 注解通过 AspectJ 自动填充 `createTime`/`updateTime`/`createUser`/`updateUser`（各服务在 `aspect/AutoFillAspect` 中定义）。
- **配置属性**：自定义配置使用 `sky.*` 前缀（`sky.jwt.*`、`sky.alioss.*`、`sky.wechat.*`、`sky.redis.*`、`sky.cache.*`）。敏感值使用 `${}` 占位符，在 `application-dev.yml` 中解析。
- **环境配置分离**：每个服务有 `application.yml`（共享配置）和 `application-dev.yml`（本地开发凭据，默认激活）。

## 多商家数据隔离

系统支持三种员工角色：`0`（超级管理员）、`1`（商家老板）、`2`（普通员工）。大部分业务实体（Dish、Setmeal、Category、Orders）都带有 `merchantId` 字段。权限控制使用 `AuthContext`：
- `AuthContext.isSuperAdmin()` — 超级管理员可查看所有商家数据
- `AuthContext.getCurrentMerchantId()` — 商家级用户只能访问自己商家的数据
- Controller 和 Service 层调用这些方法过滤查询并对写操作强制归属校验（例如商家 A 的员工不能修改商家 B 的菜品）

## 各服务注意事项

- **order-service**：使用 `@ServerEndpoint("/ws/{sid}")` WebSocket 实现实时订单通知和催单提醒。定时任务：`OrderTask.processTimeoutOrder()`（每分钟执行，取消超时 15 分钟未支付的订单）和 `processDeliveryOrder()`（每天凌晨 1 点，完成超时未配送的订单）。
- **product-service**：使用 RabbitMQ（`dish.cache.exchange` topic 交换机）广播缓存失效消息，当菜品更新/删除时触发。`DishCacheListener` 消费消息并清除 Redis 缓存键（`dishCache::{merchantId}::{categoryId}`）。同时使用 Redisson 实现分布式锁。
- **cart-service**：强制单商家购物车隔离——用户不能将不同商家的商品混在同一购物车中。
- **gateway**：排除了 DataSource 自动配置（`@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})`），因为网关没有数据库。

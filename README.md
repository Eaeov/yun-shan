# 云膳 · yun-shan — 微服务业务平台

> 基于 **Spring Cloud Alibaba** 的微服务外卖/餐饮平台。

`yun-shan`（云山）是一个基于 **Spring Cloud Alibaba** 的微服务外卖/餐饮平台，覆盖用户端（C 端点餐）与商家管理端（商家后台）。系统按"订单 / 商品 / 购物车 / 用户"四个领域拆分微服务，配合统一网关、JWT 鉴权、多商家数据隔离、WebSocket 实时通知、RabbitMQ 缓存失效广播，构成一套生产级可扩展架构。

配套 AI 智能助手层：[yun-aigc](https://github.com/<你的用户名>/yun-aigc)

---

## ✨ 核心特性

- 🚪 **统一网关鉴权**：Spring Cloud Gateway + 自定义过滤器，校验 JWT 并向下游注入 `user_info`、`gatewaySecretKey` 等头部。
- 🏬 **多商家数据隔离**：业务实体统一携带 `merchantId`，通过 `AuthContext.isSuperAdmin()` / `getCurrentMerchantId()` 在 Controller / Service / Mapper 三层过滤；商家 A 的员工无法访问商家 B 的数据。
- 🔐 **双角色 JWT**：管理端（`/admin/**`）与用户端（`/user/**`）使用独立密钥与 Token 请求头（`Token` / `Authentication`）。
- 🔌 **服务间安全调用**：每个服务实现 `GatewayKeyInterceptor` 校验内部密钥头，配合 Feign `RequestInterceptor` 自动透传上下文（Header + JWT + 用户 ID + 网关密钥）。
- 📡 **WebSocket 实时通知**：骑手取件、订单状态变更、催单提醒通过 `@ServerEndpoint("/ws/{sid}")` 实时推送，配合 Redis 状态会话管理。
- 📦 **RabbitMQ 缓存失效广播**：商品服务在菜品/套餐变更时广播失效消息，其他实例的 `DishCacheListener` 消费并清除本地 Redis 缓存。
- 🛒 **单商家购物车**：用户不能在购物车中混合不同商家商品，领域规则由购物车服务强校验。
- 🛡️ **Redisson 分布式锁**：防止商品下单、库存扣减等关键路径的并发竞争。
- ☁️ **阿里云 OSS 文件上传**：商品图片、用户头像通过统一上传接口落 OSS，凭证走配置注入不落代码。
- 📚 **Knife4j 接口文档**：每个服务自带 Swagger UI，便于对接联调。

---

## 🧰 技术栈

| 维度        | 选型                                                                          |
| --------- | --------------------------------------------------------------------------- |
| 语言 / 运行时  | Java 17                                                                     |
| 框架        | Spring Boot 3.2.4 · Spring Cloud 2023.0.1 · Spring Cloud Alibaba 2023.0.1.0 |
| 数据访问      | MyBatis-Plus 3.x · MySQL 8.0                                                |
| 缓存 / 分布式锁 | Redis · Redisson                                                            |
| 消息队列      | RabbitMQ                                                                    |
| 服务网关      | Spring Cloud Gateway（基于 Reactor WebFlux）                                    |
| 注册/配置中心   | Nacos                                                                       |
| 对象存储      | 阿里云 OSS                                                                     |
| 鉴权        | JJWT（HS256） · 自定义 Gateway Key 校验                                            |
| 接口文档      | Knife4j                                                                     |
| 构建        | Maven 多模块                                                                   |

---

## 🏗 系统架构

```
                              客户端 (小程序 / 商家后台)
                                       │
                                       ▼
                          ┌──────────────────────────┐
                          │   Gateway  (8080)         │
                          │  JWT 校验 + Header 注入    │
                          └──────────┬───────────────┘
                                     │
        ┌────────────┬───────────────┼───────────────┬────────────┐
        ▼            ▼               ▼               ▼            ▼
  user-service  cart-service  product-service  order-service  chat-ai
   (8081)        (8082)        (8084)          (8083)        (8085)
   用户/员工      购物车         商品/套餐/分类    订单/WebSocket  AI 助手
                  ──────────────────────────────────────────
                          MySQL · Redis · RabbitMQ · Nacos · OSS
```

- **yun-pojo**（`com.sky`）：实体 / DTO / VO，纯数据对象，被所有模块依赖。
- **yun-common**（`com.sky`）：统一返回 `Result<T>`、JWT 工具、`BaseContext` / `AuthContext` ThreadLocal、OSS / 微信工具、`@AutoFill` AOP、共享拦截器与配置属性类，通过 `META-INF/spring/...AutoConfiguration.imports` 自动装配。
- **yun-api**（`com.zyj.yunapi`）：所有 `@FeignClient` 接口与 `FeignConfig`，作为依赖被服务调用方引入。

---

## 🧱 模块与服务清单

| 模块 / 服务             | 端口   | 关键依赖                                                                   | 职责                                  |
| ------------------- | ---- | ---------------------------------------------------------------------- | ----------------------------------- |
| **gateway**         | 8080 | spring-cloud-starter-gateway · JJWT · yun-common                       | 路由、JWT 校验、上下文注入                     |
| **user-service**    | 8081 | MySQL · MyBatis-Plus · Redis · Knife4j · jbcrypt                       | 员工/用户/地址簿/商家账号/统计报表                 |
| **cart-service**    | 8082 | MySQL · MyBatis-Plus · Redis · Knife4j                                 | 购物车，强制单商家隔离                         |
| **order-service**   | 8083 | MySQL · MyBatis-Plus · Redis · Knife4j · WebSocket                     | 下单、订单状态机、超时关单、定时任务、WebSocket 推送     |
| **product-service** | 8084 | MySQL · MyBatis-Plus · Redis · Redisson · RabbitMQ · Knife4j · 阿里云 OSS | 商品/套餐/分类/菜品评论、RabbitMQ 缓存失效、OSS 上传  |
| **yun-pojo**        | —    | —                                                                      | 实体 / DTO / VO                       |
| **yun-common**      | —    | —                                                                      | 通用组件（Result、JWT、OSS、拦截器、异常、常量、配置属性） |
| **yun-api**         | —    | —                                                                      | OpenFeign 接口与配置                     |

---

## 🏬 多商家数据隔离

系统区分三种员工角色：`0` 超级管理员、`1` 商家老板、`2` 普通员工。**Dish / Setmeal / Category / Orders** 等核心实体均带 `merchantId`。

- `AuthContext.isSuperAdmin()` — 超级管理员可访问所有商家
- `AuthContext.getCurrentMerchantId()` — 商家级用户仅可访问所属商家
- Controller / Service 层调用上述方法过滤查询、写操作校验归属

---

## 🚀 快速开始

### 前置依赖

- JDK 17
- Maven 3.6+
- MySQL 8.0（库 `yun_shan`）
- Redis 6.0+
- RabbitMQ 3.x
- Nacos 2.x

### 1. 克隆与构建

```bash
git clone https://github.com/<your-username>/yun-shan.git
cd yun-shan
./mvnw clean install -DskipTests
```

### 2. 准备配置（Nacos 方式 · 推荐）

把本地 Nacos 配置导入（或按下方环境变量注入）：

```bash
# dataId 形如 cart-service-dev.yml / user-service-dev.yml 等
# 详细字段参见下方"环境变量清单"
```

### 3. 启动顺序

```
1. Nacos + MySQL + Redis + RabbitMQ
2. gateway（端口 8080）
3. user-service（8081）
4. product-service（8084）
5. cart-service（8082）
6. order-service（8083）
```

单独启动：

```bash
./mvnw spring-boot:run -pl gateway
./mvnw spring-boot:run -pl user-service
...
```

无 Docker Compose，请通过 IDE 或 `mvnw` 单独启动每个模块。

---

## 🔐 环境变量清单

> 所有配置已迁移至 Nacos / 环境变量；代码仓库不含任何真实密钥。请勿提交 `application-dev.yml`。

| 变量                                                                                                        | 说明                                                |
| --------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `NACOS_SERVER_ADDR`                                                                                       | Nacos 地址，例 `192.168.x.x:8848`，默认 `localhost:8848` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`                                                                  | MySQL 连接信息                                        |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`                                                            | Redis 连接                                          |
| `MQ_HOST` / `MQ_PORT` / `MQ_USERNAME` / `MQ_PASSWORD`                                                     | RabbitMQ 连接                                       |
| `SKY_JWT_ADMIN_SECRET` / `SKY_JWT_USER_SECRET`                                                            | JWT 签名密钥（256 位随机串）                                |
| `SKY_GATEWAY_SECRET`                                                                                      | 服务间调用网关密钥                                         |
| `SKY_ALIOSS_BUCKET` / `SKY_ALIOSS_ACCESS_KEY_ID` / `SKY_ALIOSS_ACCESS_KEY_SECRET` / `SKY_ALIOSS_ENDPOINT` | 阿里云 OSS                                           |
| `SKY_WECHAT_APPID` / `SKY_WECHAT_SECRET`                                                                  | 微信小程序凭证                                           |

---

## 📖 接口文档

各服务集成 **Knife4j**，启动后访问：

- 网关聚合：`http://localhost:8080/doc.html`（如已配置聚合）
- 单服务：`http://localhost:<端口>/doc.html`

---

## 🗂 目录结构

```
yun-shan/
├── gateway/                 # Spring Cloud Gateway
├── user-service/            # 用户/员工/商家/地址簿
├── cart-service/            # 购物车
├── order-service/           # 订单 + WebSocket
├── product-service/         # 商品/套餐/分类 + RabbitMQ + OSS
├── yun-pojo/                # 实体 / DTO / VO
├── yun-common/              # 通用组件（Result、JWT、Interceptor、配置属性）
├── yun-api/                 # OpenFeign 接口
├── pom.xml                  # 父工程
├── CLAUDE.md                # Claude Code 工作指导
└── README.md                # 本文件
```

---

## 📝 设计说明

- **统一返回格式**：所有 Controller 返回 `com.sky.result.Result<T>`，`code=1` 成功，`code=0` 失败。
- **AOP 自动填充**：`@AutoFill` 注解 + AspectJ 自动填充 `createTime` / `updateTime` / `createUser` / `updateUser`。
- **路由规范**：`/admin/**` 管理端、`/user/**` 用户端；网关通过路径前缀以 `lb://` 路由到对应服务。
- **WebSocket**：`@ServerEndpoint("/ws/{sid}")` 实现实时订单通知；定时任务 `OrderTask.processTimeoutOrder()` 每分钟扫描超时 15 分钟未支付订单并关闭，`processDeliveryOrder()` 每天凌晨 1 点完成超时未配送订单。
- **购物车隔离**：业务规则层强制单商家，不允许混单。

---

## 📄 License

本仓库仅用于学习与展示，请勿直接商用。如需引用请注明出处。
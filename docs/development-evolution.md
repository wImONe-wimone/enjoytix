# EnjoyTix 开发演进文档

本文基于当前 `main` 分支从首个提交到当前 HEAD 的提交历史与代码变更整理，时间线按 `git log --reverse` 顺序展开。文档重点记录项目从空仓库到当前 MVP 与数据库持久化能力的演进过程，包括模块拆分、核心功能、关键类与接口、业务流程、存储实现、配置支持、测试与修复记录，以及后续优化方向。

## 1. 当前分支提交时间线

| 顺序 | 提交 | 时间 | 主题 | 主要产物 |
| --- | --- | --- | --- | --- |
| 1 | `c4dcd72` | 2026-07-04 15:51 | 初始仓库 | 新增 `LICENSE`。 |
| 2 | `d64a05e` | 2026-07-04 15:55 | 初始化 Maven 项目 | 新增根 `pom.xml` 与 `.gitignore`。 |
| 3 | `c6c0b47` | 2026-07-04 16:25 | 初始化多模块工作区 | 建立 `dependencies`、`frameworks`、`services` 聚合模块与各服务空模块。 |
| 4 | `b7c084a` | 2026-07-04 16:30 | 添加公共框架模块 | 落地统一响应、异常、分页、追踪上下文、缓存、数据库、分布式 ID、幂等、日志、Web 自动配置等基础能力。 |
| 5 | `9dadff9` | 2026-07-04 16:32 | 添加网关路由 | 完成 Gateway 启动类、路由配置、请求 ID、开发态令牌解析与统一网关错误输出。 |
| 6 | `a30c7bd` | 2026-07-04 16:36 | 添加用户基础域 | 实现用户注册登录、用户信息查询、观演人管理与内存仓储。 |
| 7 | `74a8afb` | 2026-07-04 16:40 | 添加演出基础域 | 实现演出、艺人、场馆、场次、票档、座位图查询模型与接口。 |
| 8 | `54c9ca7` | 2026-07-04 16:44 | 实现锁座流程 | 实现余票查询、座位查询、锁座、释放、出票与票务内存仓储。 |
| 9 | `7f2d066` | 2026-07-04 16:48 | 实现订单创建与超时关闭 | 实现订单创建、取消、支付成功确认、状态流转、超时关闭与票务远程调用接口。 |
| 10 | `efee250` | 2026-07-04 16:52 | 实现模拟支付回调 | 实现支付单创建、模拟支付成功、支付详情与退款基础流程。 |
| 11 | `eb7f1ff` | 2026-07-04 16:55 | 验证 MVP 购票链路 | 新增 `tests/mvp-flow-test`，覆盖锁座、下单、支付、出票与超时释放。 |
| 12 | `bd033fc` | 2026-07-04 16:59 | 添加订单超时关闭消息抽象 | 新增订单超时消息对象、发送接口、本地监听与无操作实现。 |
| 13 | `61e8391` | 2026-07-04 17:02 | 添加分布式库存锁模板 | 在缓存框架中新增锁模板，本地与 Redisson 两种实现，并接入票务库存修改链路。 |
| 14 | `6981deb` | 2026-07-04 17:04 | 添加写接口幂等标记 | 对订单、票务、支付、退款等写接口添加 `@Idempotent`。 |
| 15 | `367333f` | 2026-07-04 17:24 | 添加中文项目指南 | 新增中文 `README.md`。 |
| 16 | `32eb705` | 2026-07-04 17:25 | 添加本地运行配置 | 新增 `.run` 下各服务本地启动配置。 |
| 17 | `f3699ef` | 2026-07-04 20:24 | 添加 MySQL 表结构与 ShardingSphere 配置 | 新增数据库脚本、种子数据、各服务数据库 profile 配置，并补充实体表映射注解。 |
| 18 | `60ffbb6` | 2026-07-04 20:35 | 拆分每服务独立数据库 | 将单库调整为用户、演出、票务、订单、支付、营销六个独立 schema，并新增服务级 ShardingSphere 配置。 |
| 19 | `8e51451` | 2026-07-04 20:45 | 添加 MyBatis-Plus Mapper 与 SQL 校验 | 为所有业务表新增 Mapper，补充营销实体与校验脚本。 |
| 20 | `2c7bdac` | 2026-07-04 20:57 | 按 mysql profile 切换仓储实现 | 抽象 Repository 接口，新增 MyBatis-Plus 实现，保留默认内存实现。 |

## 2. 工程结构演进

### 2.1 根工程与依赖管理

项目当前是 Maven 多模块工程：

- 根模块：`enjoytix-all`
- 依赖版本聚合：`dependencies`
- 公共框架聚合：`frameworks`
- 业务服务聚合：`services`
- 测试聚合：`tests`

根 `pom.xml` 固定 Java 17 编译，并统一管理 Maven Compiler、Surefire、Spring Boot Maven Plugin。`dependencies/pom.xml` 统一导入 Spring Boot 3.0.7、Spring Cloud 2022.0.3、Spring Cloud Alibaba 2022.0.0.0-RC2，并集中声明 MyBatis-Plus、ShardingSphere、Fastjson2、Hutool、Redisson、RocketMQ、JWT、Guava、Prometheus 指标依赖版本。

### 2.2 公共框架模块

`frameworks` 下当前包含：

- `base`：请求追踪与用户上下文。
  - `TraceConstants`
  - `TraceContext`
  - `UserContext`
  - `UserInfoDTO`
- `convention`：统一响应、异常、错误码与分页对象。
  - `Result`
  - `BaseErrorCode`
  - `IErrorCode`
  - `AbstractException`
  - `ClientException`
  - `ServiceException`
  - `RemoteException`
  - `PageRequest`
  - `PageResponse`
- `common`：通用枚举和工具类。
  - `DelEnum`
  - `FlagEnum`
  - `StatusEnum`
  - `Assert`
  - `BeanUtil`
- `web`：Web 层统一处理。
  - `GlobalExceptionHandler`
  - `Results`
  - `TraceIdFilter`
  - `WebAutoConfiguration`
- `database`：MyBatis-Plus 与基础实体支持。
  - `BaseDO`
  - `MybatisPlusAutoConfiguration`
  - `CustomIdGenerator`
  - `MyMetaObjectHandler`
- `distributed-id`：雪花算法 ID。
  - `IdGenerator`
  - `IdGeneratorManager`
  - `SnowflakeIdGenerator`
  - `DistributedIdProperties`
  - `DistributedIdAutoConfiguration`
- `cache`：缓存与锁。
  - `CacheKeyBuilder`
  - `DistributedCache`
  - `DistributedLockTemplate`
  - `LocalDistributedLockTemplate`
  - `RedissonDistributedLockTemplate`
  - `CacheAutoConfiguration`
- `idempotent`：接口幂等注解与切面。
  - `Idempotent`
  - `IdempotentAspect`
  - `IdempotentSceneEnum`
  - `IdempotentTypeEnum`
  - `IdempotentAutoConfiguration`
- `log`：操作日志注解与切面。
  - `OperationLog`
  - `OperationLogAspect`
  - `LogAutoConfiguration`

这些模块先于业务服务完成，形成了统一返回、统一异常、自动填充、逻辑删除、分布式 ID、幂等、日志和锁模板的基础能力。

## 3. 服务模块与职责边界

### 3.1 gateway-service

网关服务负责统一入口、路由、基础鉴权和错误响应。

关键类：

- `GatewayServiceApplication`：网关启动类。
- `RequestIdGlobalFilter`：为请求补充追踪 ID。
- `TokenValidateGatewayFilterFactory`：基于配置路径执行令牌校验。
- `DevTokenParser`：开发态令牌解析。
- `AuthUser`：网关解析出的用户信息。
- `GatewayResponseWriter`：统一写出网关响应。
- `GatewayErrorWebExceptionHandler`：统一处理网关异常。

当前路由覆盖：

- `/api/user/**` 到用户服务。
- `/api/performance/**`、`/api/show/**`、`/api/venue/**` 到演出服务。
- `/api/ticket/**` 到票务服务。
- `/api/order/**` 到订单服务。
- `/api/pay/**`、`/api/refund/**` 到支付服务。
- `/api/marketing/**` 到营销服务。

### 3.2 user-service

用户服务负责账号、登录和观演人管理。

接口入口：

- `UserController`
  - `POST /api/user/register`
  - `POST /api/user/login`
  - `GET /api/user/me`
  - `GET /api/user/{userId}`
  - `POST /api/user/logout`
- `AttendeeController`
  - `GET /api/user/attendees`
  - `POST /api/user/attendees`
  - `PUT /api/user/attendees`
  - `DELETE /api/user/attendees/{attendeeId}`

关键实现：

- `UserService` / `UserServiceImpl`
- `AttendeeService` / `AttendeeServiceImpl`
- `PasswordUtil`
- `UserDO`
- `AttendeeDO`
- `UserRepository`
- `InMemoryUserRepository`
- `MyBatisPlusUserRepository`
- `UserMapper`
- `AttendeeMapper`

当前能力包括用户注册、登录、查询当前用户、按 ID 查询用户、退出占位、观演人新增、修改、删除和列表查询。

### 3.3 performance-service

演出服务负责文娱演出基础数据查询，承载演出、艺人、场馆、场次、票档、座位图等静态或准静态数据。

接口入口：

- `PerformanceController`
  - `GET /api/performance/page`
  - `GET /api/performance/{performanceId}`
- `ShowController`
  - `GET /api/show/{showId}`
  - `GET /api/show/{showId}/ticket-categories`
  - `GET /api/show/{showId}/seat-map`

关键实现：

- `PerformanceService` / `PerformanceServiceImpl`
- `PerformanceRepository`
- `InMemoryPerformanceRepository`
- `MyBatisPlusPerformanceRepository`
- `PerformanceDO`
- `ArtistDO`
- `VenueDO`
- `HallDO`
- `ShowSessionDO`
- `TicketCategoryDO`
- `SeatMapDO`
- `SeatDO`
- `PerformanceMapper`
- `ArtistMapper`
- `VenueMapper`
- `HallMapper`
- `ShowSessionMapper`
- `TicketCategoryMapper`
- `SeatMapMapper`
- `SeatMapper`

当前能力包括演出分页筛选、演出详情、场次详情、票档列表与座位图查询。

### 3.4 ticket-service

票务服务负责库存、锁座、释放与出票，是当前高并发购票链路的核心模块。

接口入口：

- `TicketController`
  - `GET /api/ticket/availability`
  - `GET /api/ticket/seats`
  - `POST /api/ticket/lock`
  - `POST /api/ticket/release`
  - `POST /api/ticket/issue`

关键实现：

- `TicketService` / `TicketServiceImpl`
- `TicketRepository`
- `InMemoryTicketRepository`
- `MyBatisPlusTicketRepository`
- `TicketLockKeys`
- `TicketStockDO`
- `SeatStockDO`
- `TicketLockDO`
- `TicketIssueDO`
- `TicketStockMapper`
- `SeatStockMapper`
- `TicketLockMapper`
- `TicketIssueMapper`
- `SeatStockStatusEnum`
- `TicketLockStatusEnum`

核心行为：

- `availability(showId)`：查询票档余票，执行过期锁释放。
- `seats(showId)`：查询座位状态，执行过期锁释放。
- `lock(userId, request)`：按场次维度加锁，校验票档余量，校验座位是否可售，写入锁单并扣减可售库存到锁定库存。
- `release(userId, request)`：校验锁归属后释放库存与座位。
- `issue(userId, request)`：校验锁仍有效，锁定库存转为已售库存，生成电子票码，座位置为已售。

票务链路通过 `DistributedLockTemplate` 保护同一场次库存修改，当前默认可使用本地锁，也具备 Redisson 实现。

### 3.5 order-service

订单服务负责订单创建、取消、支付成功确认、订单查询与超时关闭。

接口入口：

- `OrderController`
  - `POST /api/order/create`
  - `POST /api/order/cancel`
  - `POST /api/order/pay-success`
  - `GET /api/order/{orderId}`
  - `GET /api/order/page`

关键实现：

- `OrderService` / `OrderServiceImpl`
- `OrderTimeoutCloseService`
- `TicketRemoteService`
- `OrderTimeoutMessageSender`
- `OrderTimeoutMessage`
- `LocalOrderTimeoutMessageSender`
- `LocalOrderTimeoutMessageListener`
- `NoopOrderTimeoutMessageSender`
- `OrderRepository`
- `InMemoryOrderRepository`
- `MyBatisPlusOrderRepository`
- `OrderDO`
- `OrderItemDO`
- `OrderStatusLogDO`
- `OrderMapper`
- `OrderItemMapper`
- `OrderStatusLogMapper`
- `OrderStatusEnum`

核心行为：

- 创建订单前先扫描关闭过期订单。
- 查询票档价格与余量。
- 调用票务服务锁座或锁库存。
- 创建主订单、订单明细和状态日志。
- 发送订单超时关闭消息。
- 用户取消订单时释放票务锁。
- 支付成功时调用票务出票，并把票码回填订单明细。
- 定时扫描关闭超时未支付订单。
- 接收本地超时消息时按订单号与过期时间进行幂等关闭。

### 3.6 pay-service

支付服务负责支付单、模拟支付成功和退款基础流程。

接口入口：

- `PayController`
  - `POST /api/pay/create`
  - `POST /api/pay/mock-success`
  - `GET /api/pay/{payId}`
- `RefundController`
  - `POST /api/refund/apply`

关键实现：

- `PayService` / `PayServiceImpl`
- `OrderRemoteService`
- `PayRepository`
- `InMemoryPayRepository`
- `MyBatisPlusPayRepository`
- `PayDO`
- `RefundDO`
- `PayMapper`
- `RefundMapper`
- `PayStatusEnum`
- `RefundStatusEnum`

核心行为：

- 同一订单重复创建支付单时返回已有支付单。
- 仅允许待支付订单创建支付单。
- 模拟支付成功后通知订单服务执行支付确认与出票。
- 重复支付成功回调返回已有成功支付单。
- 退款接口当前完成支付单状态切换与退款单落库。

### 3.7 marketing-service

营销服务当前完成了数据层骨架，便于后续扩展预售、限购、优惠券和活动规则。

当前类：

- `MarketingActivityDO`
- `PurchaseLimitRuleDO`
- `CouponDO`
- `UserCouponDO`
- `MarketingActivityMapper`
- `PurchaseLimitRuleMapper`
- `CouponMapper`
- `UserCouponMapper`

当前尚未实现对外 Controller 与业务 Service。

### 3.8 aggregation-service

聚合服务当前保留 Maven 模块骨架，用于后续本地聚合启动或 MVP 联调封装。当前未实现应用代码。

## 4. 核心业务流程

### 4.1 注册登录与观演人维护

1. 用户调用 `POST /api/user/register` 注册账号。
2. `UserServiceImpl` 校验用户名唯一性，使用 `PasswordUtil` 生成密码摘要。
3. 登录时校验密码，返回用户基础信息与开发态 token。
4. 用户通过观演人接口维护实名观演人信息。

### 4.2 演出浏览与选座前查询

1. 用户调用 `GET /api/performance/page` 按城市、类型、艺人、场馆、日期筛选演出。
2. 进入详情后调用 `GET /api/performance/{performanceId}` 查询演出、艺人、场馆、场次信息。
3. 调用 `GET /api/show/{showId}/ticket-categories` 查询票档。
4. 调用 `GET /api/show/{showId}/seat-map` 查询座位图。
5. 调用 `GET /api/ticket/availability` 与 `GET /api/ticket/seats` 查询实时票档余量和座位库存状态。

### 4.3 锁座或锁库存

1. 用户选择具体座位或只选择票档数量。
2. 订单服务创建订单时通过 `TicketRemoteService` 调用票务锁定。
3. `TicketServiceImpl.lock` 按 `showId` 生成库存锁 key。
4. 服务先释放已过期锁，再校验票档可用库存。
5. 如果有座位，逐个校验座位属于当前票档且状态为 `AVAILABLE`。
6. 座位状态置为 `LOCKED`，票档 `locked_stock` 增加。
7. 生成 `TicketLockDO`，状态为 `LOCKED`，并写入过期时间。

### 4.4 创建待支付订单

1. `OrderServiceImpl.create` 查询票档价格。
2. 调用票务锁定成功后生成订单号、订单主表、订单明细和状态日志。
3. 订单状态为 `PENDING_PAYMENT`。
4. 支付过期时间沿用票务锁过期时间。
5. 发送订单超时关闭消息，默认实现可在本地监听中关闭订单。

### 4.5 模拟支付与出票

1. 用户调用 `POST /api/pay/create` 创建支付单。
2. 支付服务查询订单详情，确认订单仍为 `PENDING_PAYMENT`。
3. 用户调用 `POST /api/pay/mock-success` 模拟支付成功。
4. 支付服务调用订单服务 `pay-success`。
5. 订单服务校验订单未超时且仍待支付。
6. 订单服务调用票务服务出票。
7. 票务服务把锁定库存转为已售库存，座位转为 `SOLD`，生成票码。
8. 订单服务回填票码并把订单状态切换为 `PAID`。
9. 支付服务把支付单状态切换为 `SUCCESS`。

### 4.6 超时关闭与释放

当前具备两种关闭入口：

- 订单服务定时扫描 `closeExpiredOrders()`。
- 本地超时消息监听触发 `closeIfExpired(orderId, expectedExpireTime, reason)`。

关闭时订单服务调用票务释放接口，票务服务将：

- 票档 `locked_stock` 减少。
- 已锁座位恢复为 `AVAILABLE`。
- 锁单状态切换为 `RELEASED` 或 `EXPIRED`。
- 订单状态切换为 `CLOSED`，写入状态日志。

### 4.7 退款基础流程

1. 用户调用 `POST /api/refund/apply`。
2. 支付服务校验支付单属于当前用户且状态为 `SUCCESS`。
3. 支付单状态切换为 `REFUNDED`。
4. 生成退款单，状态为 `SUCCESS`。

当前退款流程尚未反向影响订单状态与票务状态。

## 5. 数据存储实现演进

### 5.1 内存仓储阶段

早期 MVP 为降低联调成本，各服务直接使用内存仓储：

- `InMemoryUserRepository`
- `InMemoryPerformanceRepository`
- `InMemoryTicketRepository`
- `InMemoryOrderRepository`
- `InMemoryPayRepository`

这种实现支持单进程内快速验证业务状态机，配合测试模块完成购票主链路验证。

### 5.2 数据库脚本阶段

后续新增 `scripts/mysql` 目录：

- `00-create-databases.sql`：创建每个微服务独立 schema。
- `01-schema.sql`：创建所有业务表。
- `02-seed-data.sql`：写入用户、演出、座位、库存、订单、支付、营销等样例数据。
- `03-verify-data.sql`：校验 schema、表和核心样例数据数量。
- `README.md`：说明脚本执行顺序和用途。

当前 schema 拆分如下：

- `enjoytix_user`
  - `et_user`
  - `et_attendee`
- `enjoytix_performance`
  - `et_artist`
  - `et_venue`
  - `et_seat_map`
  - `et_hall`
  - `et_performance`
  - `et_show_session`
  - `et_ticket_category`
  - `et_seat`
- `enjoytix_ticket`
  - `et_ticket_stock`
  - `et_seat_stock`
  - `et_seat_lock`
  - `et_ticket_issue`
- `enjoytix_order`
  - `et_order`
  - `et_order_item`
  - `et_order_status_log`
- `enjoytix_pay`
  - `et_pay_order`
  - `et_refund_order`
- `enjoytix_marketing`
  - `et_marketing_activity`
  - `et_purchase_limit_rule`
  - `et_coupon`
  - `et_user_coupon`

### 5.3 MyBatis-Plus 与 ShardingSphere 配置阶段

每个业务服务新增 `application-mysql.yaml`，通过 Spring profile 启用真实数据库连接。配置统一使用 ShardingSphere Driver，并指向服务自己的 `shardingsphere-*.yaml`：

- `user-service`
  - `application-mysql.yaml`
  - `shardingsphere-user.yaml`
- `performance-service`
  - `application-mysql.yaml`
  - `shardingsphere-performance.yaml`
- `ticket-service`
  - `application-mysql.yaml`
  - `shardingsphere-ticket.yaml`
- `order-service`
  - `application-mysql.yaml`
  - `shardingsphere-order.yaml`
- `pay-service`
  - `application-mysql.yaml`
  - `shardingsphere-pay.yaml`
- `marketing-service`
  - `application-mysql.yaml`
  - `shardingsphere-marketing.yaml`

ShardingSphere 配置当前采用每服务一个数据源，并通过 `SINGLE` 规则接管该服务 schema 下的表；Hikari 连接池配置包括最大连接数、最小空闲、连接超时、空闲超时、连接最大存活时间、保活时间和连接池名称。

各服务的 `application-mysql.yaml` 同时补充：

- ShardingSphere Driver datasource。
- MyBatis-Plus mapper locations。
- 实体 type aliases package。

### 5.4 Mapper 与 Repository 切换阶段

提交 `8e51451` 为所有业务表补齐 MyBatis-Plus Mapper；提交 `2c7bdac` 进一步抽象 Repository 接口，并提供两套实现：

- 默认 profile：`InMemory*Repository`，使用 `@Profile("!mysql")`。
- `mysql` profile：`MyBatisPlus*Repository`，使用 `@Profile("mysql")`。

服务层只依赖接口：

- `UserRepository`
- `PerformanceRepository`
- `TicketRepository`
- `OrderRepository`
- `PayRepository`

这使业务逻辑不关心底层存储来源，可以在本地快速测试和真实数据库持久化之间切换。

## 6. 高并发与一致性设计现状

当前已落地的能力：

- 按场次维度保护库存修改：`TicketLockKeys.showStock(showId)`。
- 锁模板抽象：`DistributedLockTemplate`。
- 本地锁实现：`LocalDistributedLockTemplate`。
- Redisson 锁实现：`RedissonDistributedLockTemplate`。
- 写接口幂等注解：`@Idempotent`。
- 订单超时关闭消息抽象：`OrderTimeoutMessageSender` 与 `OrderTimeoutMessage`。
- 订单状态日志：`et_order_status_log`。
- 票务锁状态：`LOCKED`、`RELEASED`、`EXPIRED`、`ISSUED`。
- 座位库存状态：`AVAILABLE`、`LOCKED`、`SOLD`。
- 支付重复成功回调幂等返回。
- 支付单按订单去重创建。
- 单次锁定和单次下单数量上限校验。

当前仍属于基础实现或待增强的能力：

- `@Idempotent` 已接入接口，但幂等 key 策略、存储策略和异常恢复还需要进一步完善。
- 订单超时消息当前有本地抽象，尚未接入真实延迟消息消费链路。
- 网关已有 Sentinel 配置入口，但限流规则、热点参数限流和降级策略尚未沉淀为代码或配置模板。
- Redis 缓存抽象与 Redisson 锁已具备，但余票缓存、座位缓存、缓存预热和防穿透策略尚未完整实现。
- ShardingSphere 当前用于服务级数据源接管，分表规则、分库路由和读写分离策略尚未配置。
- 营销限购规则已有表和 Mapper，暂未参与下单链路。

## 7. 配置与环境支持

### 7.1 服务基础配置

各业务服务提供 `application.yaml`，包含：

- 服务端口。
- Spring application name。
- Nacos discovery/config 本地默认地址。
- Actuator endpoint 暴露。
- Metrics application tag。

网关额外包含：

- Spring Cloud Gateway routes。
- Sentinel transport dashboard 默认地址。
- 需要鉴权的路径前缀配置。

### 7.2 数据库 profile

启用 `mysql` profile 后，服务读取对应的 `application-mysql.yaml` 与 `shardingsphere-*.yaml`，连接各自独立 schema。数据库脚本负责初始化结构和样例数据。

### 7.3 本地运行配置

仓库包含 `.run` 下的服务启动配置，用于 IDE 直接启动网关、用户、演出、票务、订单和支付服务。文档不展开其中的连接细节。

## 8. 测试与修复记录

### 8.1 MVP 购票链路测试

`tests/mvp-flow-test/src/test/java/com/wimone/enjoytix/tests/mvp/MvpPurchaseFlowTest.java` 覆盖两条关键路径：

- `shouldLockSeatsCreateOrderPayAndIssueTickets`
  - 初始化票务、订单、支付服务与本地远程调用适配器。
  - 用户选择座位。
  - 创建订单，断言订单为 `PENDING_PAYMENT`。
  - 创建支付单，断言支付单为 `WAITING`。
  - 模拟支付成功，断言支付单为 `SUCCESS`。
  - 查询订单，断言订单为 `PAID` 且票码数量正确。
  - 查询座位，断言座位状态为 `SOLD`。
- `shouldCloseExpiredOrderAndReleaseLockedSeats`
  - 使用 0 分钟锁定有效期。
  - 创建订单后触发过期关闭。
  - 断言订单状态为 `CLOSED`。
  - 断言座位恢复为 `AVAILABLE`。

### 8.2 数据库结构修复

提交 `60ffbb6` 将数据库从单 schema 调整为每服务独立 schema，并同步修改：

- 创建数据库脚本。
- 建表脚本。
- 种子数据脚本。
- 每服务 ShardingSphere 配置。
- 数据库脚本说明。

这次修复让服务边界与数据库边界保持一致，避免多个微服务共享同一个业务 schema。

### 8.3 SQL 校验补充

提交 `8e51451` 新增 `03-verify-data.sql`，用于校验：

- 六个 schema 是否存在。
- 关键业务表数量。
- 样例数据是否写入。

该提交同时补齐所有业务表 Mapper，使数据库结构可以被代码层直接访问。

### 8.4 仓储切换修复

提交 `2c7bdac` 把服务层从直接依赖内存仓储调整为依赖 Repository 接口。该修复解决了真实数据库持久化无法无侵入切换的问题，形成了默认内存、`mysql` profile 数据库持久化的双实现结构。

## 9. 当前实现状态

已完成：

- Java 17 + Spring Boot 3 + Spring Cloud 多模块基础工程。
- Maven 依赖版本统一管理。
- 公共框架骨架与自动配置。
- 网关服务基础路由、鉴权占位和统一错误响应。
- 用户注册登录与观演人管理。
- 演出、艺人、场馆、场次、票档和座位图查询。
- 票档余票、座位库存、锁座、释放和出票。
- 订单创建、取消、支付成功确认、超时关闭、订单查询。
- 支付单创建、模拟支付成功、支付查询和退款基础流程。
- 本地 MVP 链路测试。
- 每服务独立数据库 schema。
- 完整业务表 SQL、样例数据和校验脚本。
- MyBatis-Plus Mapper。
- Repository 抽象与内存/数据库双实现。
- `mysql` profile 持久化切换。

部分完成：

- 营销服务仅完成实体、Mapper 和数据库表，业务接口未实现。
- 订单超时关闭具备消息抽象与本地实现，尚未接入真实消息中间件。
- 分布式锁具备本地与 Redisson 实现，库存缓存体系尚未完整落地。
- ShardingSphere 已作为数据源驱动接入，实际分片策略尚未落地。
- 网关具备 Sentinel 配置入口，限流降级规则尚未体系化。
- 聚合服务仅保留模块骨架。

尚未完成：

- 完整营销限购、优惠券核销、活动规则下单校验。
- 热门演出缓存预热、余票缓存、座位图缓存和缓存一致性策略。
- 真实延迟消息关闭订单、消息重试、死信处理和消息幂等消费。
- 多实例下全链路压测与热点场次削峰策略。
- 支付退款与订单、票务之间的反向一致性。
- 接口文档与更完整的集成测试。

## 10. 后续可优化方向

建议按风险和收益分阶段推进：

1. 完成真实消息链路。
   - 接入延迟消息关闭订单。
   - 增加消息消费幂等、重试、死信和补偿任务。
   - 将本地消息实现保留为测试 profile。

2. 强化库存缓存与一致性。
   - 为票档余票、座位状态、热门演出详情设计 Redis key。
   - 增加开售前缓存预热。
   - 使用 Lua 或 Redisson 原子能力保护热点库存扣减。
   - 建立数据库与缓存修复任务。

3. 完善幂等组件。
   - 明确 request token、SpEL 参数、用户维度、业务单号等 key 策略。
   - 支持成功结果缓存和并发处理中状态。
   - 覆盖下单、锁座、支付回调、退款申请等关键接口。

4. 落地限购与风控。
   - 将 `et_purchase_limit_rule` 接入订单创建链路。
   - 增加用户维度、证件维度、场次维度的限购校验。
   - 网关层补充基础频控、热点参数限流和降级响应。

5. 推进分库分表策略。
   - 订单按 `order_id` 或 `user_id` 分表。
   - 票务库存按 `show_id` 分表。
   - 支付单按 `order_id` 或 `pay_id` 分表。
   - 补充分片键约束和跨分片查询规约。

6. 扩展服务级集成测试。
   - 增加数据库 profile 下的 Repository 测试。
   - 增加服务间 Feign 契约测试。
   - 增加并发锁座与防超卖测试。
   - 增加超时消息关闭测试。

7. 补齐文档与运维说明。
   - 输出接口清单。
   - 输出数据库 ER 关系说明。
   - 输出本地启动顺序。
   - 输出 profile、依赖中间件和排障说明。

## 11. 总结

当前项目已经从空仓库演进为具备清晰模块边界的文娱票务 MVP：网关、用户、演出、票务、订单、支付和营销骨架均已拆分；公共框架覆盖响应、异常、日志、幂等、缓存、锁、数据库和 ID；主链路已能完成选座、锁座、下单、模拟支付、出票和超时释放；数据层已从内存仓储演进到每服务独立 schema、MyBatis-Plus Mapper 和 profile 切换的真实持久化结构。

下一阶段重点应放在真实消息、缓存一致性、幂等闭环、限购风控和分片策略上，把当前可运行的 MVP 逐步增强为面向热点开售场景的高并发系统。

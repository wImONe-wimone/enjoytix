# EnjoyTix 文娱票务系统

EnjoyTix 是一个面向文娱演出场景的在线购票与订座系统。项目采用 Java 17、Spring Boot 3、Spring Cloud Alibaba 和 Maven 多模块结构，当前优先交付可验证的 MVP 主链路，并为后续接入真实中间件和数据库保留扩展边界。

当前版本使用内存仓储支撑本地验证，核心购票流程可以在不依赖外部基础设施的情况下通过自动化测试完成。服务拆分、DTO、领域模型、锁抽象、幂等组件、缓存组件和数据库基础实体已按后续生产化演进预留。

## 模块结构

```text
enjoytix-all
  dependencies                 统一依赖版本管理
  frameworks
    base                       基础常量与通用能力
    convention                 统一返回、异常、分页对象
    common                     通用扩展模块
    web                        Web 统一异常与响应处理
    distributed-id             分布式 ID 生成
    database                   BaseDO、MyBatis-Plus 基础配置
    cache                      缓存封装、本地锁与分布式锁模板
    idempotent                 幂等注解与切面
    log                        操作日志注解与切面
  services
    gateway-service            网关路由、开发鉴权占位
    user-service               用户注册登录、观演人管理
    performance-service        演出、艺人、场馆、场次、票档、座位图
    ticket-service             余票查询、座位查询、锁座、释放、出票
    order-service              下单、取消、支付成功确认、超时关闭
    pay-service                支付单、模拟支付、退款
    marketing-service          营销扩展占位
    aggregation-service        聚合验证扩展占位
  tests
    mvp-flow-test              MVP 购票链路测试
```

## 已实现范围

- 用户注册、登录、当前用户查询。
- 观演人新增、更新、删除、列表查询。
- 演出列表、演出详情、场次、票档、座位图查询。
- 票档库存、座位库存、锁座、释放、出票。
- 创建订单、取消订单、订单详情、订单列表、支付成功确认。
- 订单超时关闭，包含定时扫描兜底和本地延迟消息入口。
- 创建支付单、模拟支付成功、基础退款。
- 网关路由和开发鉴权过滤器。
- 统一返回、统一异常、参数校验、操作日志、分布式 ID、缓存、幂等、数据库基础模块。
- 库存锁模板，当前支持本地实现，并保留外部锁实现扩展点。
- MVP 测试覆盖锁座、下单、支付、出票、超时释放。

## 后续增强范围

- 将内存仓储替换为 MyBatis-Plus Mapper 和真实数据库表。
- 补充数据库迁移脚本和分片配置。
- 将本地延迟消息替换为消息队列延迟消息。
- 增加热门演出库存缓存、缓存预热和缓存失效策略。
- 增加热点接口限流、熔断、降级和排队策略。
- 增加真实支付渠道、回调验签和退款状态流转。
- 增加中间件集成测试和完整接口文档。

## 本地验证

运行全量编译和测试：

```powershell
mvn test
```

只运行 MVP 购票链路测试：

```powershell
mvn -pl tests/mvp-flow-test -am test
```

MVP 测试会在同一进程内直接组装核心服务，用于验证主链路行为，不要求提前启动外部服务。

## 服务端口

```text
gateway-service       9000
user-service          9010
performance-service   9020
ticket-service        9030
order-service         9040
pay-service           9050
```

通过网关访问受保护接口时，开发鉴权使用如下格式：

```text
Authorization: Bearer dev-1
```

网关会向后端服务转发：

```text
X-User-Id: 1
```

## 运行配置

根目录 `.run/` 下提供了 IntelliJ 共享运行配置，覆盖当前 6 个可启动服务：

```text
EnjoyTix Gateway Service
EnjoyTix User Service
EnjoyTix Performance Service
EnjoyTix Ticket Service
EnjoyTix Order Service
EnjoyTix Pay Service
```

`marketing-service` 和 `aggregation-service` 当前是占位模块，暂未提供应用启动类。

## 种子数据

演出服务和票务服务当前内置匹配的测试数据。

```text
performance 1001  Aurora Band 2026 Live
show        2001
categories  3001 VIP       1280.00
            3002 A Zone     880.00
            3003 B Zone     580.00
sample seats 400101, 400102, 400103

performance 1002  Night Train Drama
show        2002
category    3004 Standard  280.00
```

## 核心接口

网关默认地址：

```text
http://localhost:9000
```

主要接口：

```text
POST /api/user/register
POST /api/user/login
GET  /api/user/me
GET  /api/user/attendees
POST /api/user/attendees

GET  /api/performance/page
GET  /api/performance/{performanceId}
GET  /api/show/{showId}
GET  /api/show/{showId}/ticket-categories
GET  /api/show/{showId}/seat-map

GET  /api/ticket/availability?showId=2001
GET  /api/ticket/seats?showId=2001
POST /api/ticket/lock
POST /api/ticket/release
POST /api/ticket/issue

POST /api/order/create
POST /api/order/cancel
POST /api/order/pay-success
GET  /api/order/{orderId}
GET  /api/order/page

POST /api/pay/create
POST /api/pay/mock-success
GET  /api/pay/{payId}
POST /api/refund/apply
```

下单请求示例：

```json
{
  "showId": 2001,
  "categoryId": 3001,
  "seatIds": [400101, 400102]
}
```

## 核心购票链路

1. 用户通过网关发起下单请求。
2. 订单服务查询票务服务确认票档可售。
3. 票务服务按场次维度加锁，校验库存和座位状态。
4. 锁定票档库存和具体座位，返回锁定结果。
5. 订单服务生成待支付订单，并发送超时关闭消息。
6. 支付服务创建支付单并模拟支付成功。
7. 订单服务收到支付成功通知后调用票务服务出票。
8. 票务服务将锁定库存转为已售，座位转为已售。
9. 订单服务更新订单为已支付。
10. 超时未支付订单会关闭，并释放锁定库存和座位。

## 状态流转

```text
座位：AVAILABLE -> LOCKED -> SOLD
座位：AVAILABLE -> LOCKED -> AVAILABLE

票务锁：LOCKED -> ISSUED
票务锁：LOCKED -> RELEASED
票务锁：LOCKED -> EXPIRED

订单：PENDING_PAYMENT -> PAID
订单：PENDING_PAYMENT -> CANCELED
订单：PENDING_PAYMENT -> CLOSED

支付单：WAITING -> SUCCESS
支付单：SUCCESS -> REFUNDED
```

## 分片建议

```text
t_order_{00..15}          按 order_id 或 user_id 分片
t_order_item_{00..15}     按 order_id 分片
t_pay_{00..15}            按 order_id 分片
t_ticket_lock_{00..15}    按 show_id 分片
t_ticket_issue_{00..15}   按 order_id 分片
t_seat_stock_{00..31}     按 show_id 分片
t_ticket_stock_{00..31}   按 show_id 分片
```

## 下一步计划

1. 实现 MyBatis-Plus Mapper、SQL 脚本和数据初始化脚本。
2. 补充分库分表运行配置，并验证订单、支付、票务链路。
3. 将本地超时消息替换为中间件延迟消息。
4. 实现热门演出缓存预热、库存缓存更新和缓存一致性处理。
5. 增加网关和热门票务接口的限流降级规则。
6. 增加完整接口文档和集成测试。

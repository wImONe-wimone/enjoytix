# Phase 4 Agent Transaction Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将已有的购票草案与一次性确认机制接入 Agent API，形成“创建草案 → 明确确认 → 创建订单”的可测试端到端交易链路。

**Architecture:** Agent 服务负责用户上下文、草案生命周期、参数指纹和一次性确认令牌；订单服务只接受专用内部请求，并从可信的服务间鉴权上下文获取用户身份。交易 API 分离预览、确认和下单动作，任何未确认或参数变化的请求都不能进入订单创建逻辑。

**Tech Stack:** Java 17, Spring Boot 3, Spring MVC, Spring Cloud OpenFeign, Sa-Token 用户上下文, JUnit 5, Mockito, AssertJ。

**Spec:** 当前 Phase 4 交易型 Agent 实施计划。

## Global Constraints

- 不实现真实支付、锁座回滚或退款执行；本计划只完成安全的下单前置链路。
- 客户端请求不得携带或覆盖 `userId`；用户身份只能来自 `AgentUserContextHolder.requireCurrent()`。
- 未明确确认不能调用订单创建服务。
- 草案的演出、票档、座位、数量和价格任一变化都会使旧确认失效。
- 确认令牌只能消费一次；跨用户、过期令牌和重放请求必须拒绝。
- 远程订单失败不得向 Agent 用户暴露下游内部错误细节。
- 修改代码前先写失败测试，并使用 `apply_patch` 修改文件。

---

### Task 1: 固化 Agent 交易 API 契约

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/api/AgentTradeController.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/trade/PurchaseDraftResponse.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/trade/PurchaseConfirmationRequest.java`
- Create: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/api/AgentTradeControllerContractTest.java`

**Interfaces:**
- `POST /api/agent/trade/drafts` 接收 `PurchaseDraftRequest`，返回草案。
- `POST /api/agent/trade/confirmations` 接收 `draftId`，返回一次性确认令牌。
- `POST /api/agent/trade/orders` 接收 `draftId`、确认令牌和购票参数，调用 `PurchaseOrderService`。
- 三个接口都从认证适配层建立上下文，不接受 `userId` 请求字段。

- [ ] **Step 1: Write the failing contract test**

  反射验证三个路径均存在、写接口使用 `POST`、请求 DTO 没有 `userId`，并验证 Controller 方法没有 `@RequestHeader(X-User-Id)` 形式的客户端用户输入。

- [ ] **Step 2: Run the contract test**

  Run: `mvn -pl services/agent-service '-Dtest=AgentTradeControllerContractTest' test`

  Expected: FAIL because the controller and request DTOs do not exist。

- [ ] **Step 3: Implement the minimal controller**

  Controller 只负责校验请求、调用 `PurchaseDraftService`/`PurchaseOrderService` 和转换响应，不把用户 ID 作为方法参数传入业务层。

- [ ] **Step 4: Run the contract and MVC tests**

  Run: `mvn -pl services/agent-service '-Dtest=AgentTradeControllerContractTest,AgentTradeControllerTest' test`

  Expected: PASS。

- [ ] **Step 5: Commit the isolated API contract**

  Commit message: `feat(agent): expose guarded trade workflow api`

### Task 2: 注册交易服务并验证确认顺序

**Files:**
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/tool/AgentToolConfiguration.java`
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/trade/InMemoryPurchaseDraftService.java`
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/trade/PurchaseOrderService.java`
- Create: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/trade/PurchaseOrderSpringConfigurationTest.java`

**Interfaces:**
- Spring 容器提供 `PurchaseDraftService` 和 `PurchaseOrderService` Bean。
- `PurchaseOrderService.create(String draftId, String confirmationToken, PurchaseDraftRequest request)` 在订单远程调用前必须完成确认令牌消费。

- [ ] **Step 1: Write the failing Spring wiring test**

  启动最小 Spring 上下文，断言两个服务 Bean 存在，并用 Mockito 验证确认失败时 `AgentOrderCreateRemoteService.create(...)` 调用次数为零。

- [ ] **Step 2: Run the wiring test**

  Run: `mvn -pl services/agent-service '-Dtest=PurchaseOrderSpringConfigurationTest' test`

  Expected: FAIL if Bean 尚未注册或调用顺序不满足要求。

- [ ] **Step 3: Register and wire the services**

  使用现有配置类注册默认草案存储和订单编排 Bean；保留测试中可注入替代实现的构造函数。

- [ ] **Step 4: Run trade tests**

  Run: `mvn -pl services/agent-service '-Dtest=*Trade*Test,*Purchase*Test' test`

  Expected: PASS。

- [ ] **Step 5: Commit service wiring**

  Commit message: `feat(agent): wire guarded purchase orchestration`

### Task 3: 加强订单服务间鉴权边界

**Files:**
- Modify: `services/order-service/src/main/java/com/wimone/enjoytix/order/controller/OrderInternalController.java`
- Create or modify: `services/order-service/src/main/java/com/wimone/enjoytix/order/auth/AgentServiceAuthentication.java`
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/auth/AgentFeignUserContextInterceptor.java`
- Create: `services/order-service/src/test/java/com/wimone/enjoytix/order/controller/OrderInternalAgentSecurityTest.java`

**Interfaces:**
- Agent 专用端点继续使用 `/api/order/internal/agent/create`。
- 服务身份通过现有 Sa-Token/内部服务鉴权配置校验；`X-Agent-Service` 只能作为路由标识，不能单独视为生产信任根。
- 用户 ID 仍由 Agent 服务上下文注入，订单服务不得读取请求体中的用户 ID。

- [ ] **Step 1: Write security failure tests**

  测试缺少服务凭证、服务凭证错误、缺少用户上下文和请求体包含 `userId` 时均拒绝，并验证普通 `/api/order/create` 与 Agent 专用端点职责隔离。

- [ ] **Step 2: Run security tests**

  Run: `mvn -pl services/order-service '-Dtest=OrderInternalAgentSecurityTest' test`

  Expected: FAIL until the service authentication check exists。

- [ ] **Step 3: Implement the smallest compatible service check**

  优先复用仓库已有内部鉴权配置和请求头约定；如果当前环境只有开发态标识，则明确将其封装为适配层，并保留生产配置入口，不在 Controller 中散落密钥逻辑。

- [ ] **Step 4: Run order controller tests**

  Run: `mvn -pl services/order-service '-Dtest=*Order*Test' test`

  Expected: PASS。

- [ ] **Step 5: Commit service boundary hardening**

  Commit message: `security(order): protect agent order endpoint`

### Task 4: 完成端到端越权、篡改和重放验收

**Files:**
- Modify: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/trade/PurchaseDraftServiceTest.java`
- Modify: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/trade/PurchaseOrderServiceTest.java`
- Create: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/api/AgentTradeSecurityFlowTest.java`

**Interfaces:**
- 验收场景覆盖：用户 A 创建草案、用户 B 尝试确认、价格变化、座位变化、过期、确认后重放、确认后远程调用失败。

- [ ] **Step 1: Add failing security-flow cases**

  每个场景都断言订单远程服务调用次数；除首次合法确认外，次数必须为零。

- [ ] **Step 2: Run focused security tests**

  Run: `mvn -pl services/agent-service '-Dtest=PurchaseDraftServiceTest,PurchaseOrderServiceTest,AgentTradeSecurityFlowTest' test`

  Expected: FAIL only for未覆盖的安全行为。

- [ ] **Step 3: Fix root-cause state transitions**

  只调整草案/令牌状态管理，不通过测试专用绕过方式放宽校验。

- [ ] **Step 4: Run full Agent and order verification**

  Run: `mvn -pl services/agent-service,services/order-service -am test` and `git diff --check`

  Expected: all tests pass and no whitespace errors。

- [ ] **Step 5: Commit the Phase 4 executable workflow**

  Commit message: `feat(agent): complete guarded order creation flow`

### Task 5: 暂缓退款相关设计

本阶段不设计、不实现 Agent 退款草案、退款确认接口或退款执行流程。现有普通退款、退款完成和退款回滚接口保持不变，退款能力在后续单独立项并获得明确授权后再分析。

## Self-Review Checklist

- [ ] API 层没有客户端可控的 `userId`。
- [ ] 订单远程调用只能发生在确认成功之后。
- [ ] 确认令牌跨用户、过期、篡改和重放均失败。
- [ ] Feign 客户端上下文 ID 不重复。
- [ ] 订单服务内部入口具备独立服务身份校验。
- [ ] 未实现支付、真实锁座回滚和任何 Agent 退款接口；这些属于后续明确授权的独立变更。

# Agent Platform Phased Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Agent 原型推进为具备稳定契约、用户隔离、可持久化运行记录、只读工具和可治理扩展点的分阶段 Agent 平台。

**Architecture:** 保留现有多模块父工程和服务调用方式，优先让 `agent-service` 采用受控的专属依赖管理；不在没有兼容性 POC 结论前升级全局 Spring Boot。Agent 对外 API、内部服务 API 和 Tool API 分层，所有对话与工具调用都通过统一用户上下文和审计边界。

**Tech Stack:** Java 17、Spring Boot 3.0.x、Spring Cloud 2022.0.x、Spring Cloud OpenFeign、Spring AI（完成兼容性 POC 后定版）、MCP Java SDK（完成兼容性 POC 后定版）、Sa-Token、SSE、Micrometer、PostgreSQL/PgVector、RustFS、Apache Tika、Redis/Redisson。

**Spec:** 用户提供的 Agent Phase 0–6 实施要求。

## Global Constraints

- 当前父工程使用 Spring Boot `3.0.7`、Spring Cloud `2022.0.3`、Java `17`；未经 POC 证明不得全局升级。
- Agent 内部查询必须携带可信用户上下文；服务端不得信任客户端直接传入的 `X-User-Id`。
- Phase 3 之前所有 Agent Tool 必须只读，不得产生订单、锁座、支付或退款副作用。
- 交易型操作必须采用参数绑定的、一次性确认令牌；确认参数发生任何变化时令牌失效。
- 每个阶段必须有定向测试和可重复的验收命令；不修复与本阶段无关的既有日期敏感测试。

## 当前阶段判断

- Phase 0：兼容性 POC 已通过；Phase 0-B 契约与统一鉴权仍在实施，主工程依赖迁移尚未决定。
- Phase 1：已有 Conversation、Message、模型抽象和 SSE 原型；缺少明确 Run 生命周期、稳定存储、真实模型验收和完整取消/错误审计。
- Phase 2：尚未开始。
- Phase 3：早期骨架，已有 Tool Registry、演出/场次查询和自定义 MCP SSE，但工具覆盖、协议兼容、治理和审计不足。
- Phase 4–6：尚未开始。

## File Map

- Modify: `services/agent-service/pom.xml` — Agent 专属依赖与版本覆盖。
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/model/AgentRun.java` — Run 状态模型。
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/service/RunRepository.java` — Run 存储端口。
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/service/InMemoryRunRepository.java` — MVP 测试实现。
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/service/AgentApplicationService.java` — Run 生命周期和 SSE 取消记录。
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/auth/AgentUserContext.java` — 统一用户上下文接口。
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/api/AgentApiContract.java` — API 分层与路径常量。
- Create: `docs/architecture/agent-contracts.md` — 公开、内部、Tool API 契约说明。

## Phase 0-A: Compatibility POC

- [x] 在 `pocs/agent-compatibility-poc` 建立不加入根 Reactor 的隔离 Agent POC。
- [x] 验证 Spring Boot `3.5.15`、Spring Cloud `2025.0.0`、Spring AI `1.0.9`、MCP SDK `0.18.3` 与 Sa-Token `1.45.0` 的组合。
- [x] 启动 Agent 最小上下文，验证模型 starter、MCP WebMVC/SSE transport 和 Sa-Token 同时加载。
- [x] 验证 `enjoytix-web`、`enjoytix-distributed-id` 二进制兼容及 Maven dependency convergence。
- [ ] 评估将通过验证的 Agent 依赖平台落到主 `agent-service` 的迁移方案；在方案确定前不升级全局父工程。

## Phase 0-B: Contracts and Auth

- [ ] 将 Agent 对话、消息、Run、工具结果定义为稳定内部 DTO，字段使用版本化兼容策略。
- [ ] 公开 API 使用 `/api/agent/**`，内部服务调用使用 `/internal/agent/**`，MCP Tool API 使用 `/mcp/**`；明确每类 API 的认证和审计要求。
- [ ] 将 Gateway 解析出的登录身份转换为 Agent `AgentUserContext`，内部 Feign 只允许从上下文注入可信用户头。
- [ ] 以 Sa-Token 为统一鉴权适配层；在依赖未定版前保留可替换的 `AgentAuthenticationPort`，不得在 Controller 复制鉴权逻辑。
- [ ] 为演出、票务、订单、用户查询补齐只读内部接口、DTO 和越权测试。

## Phase 1: Conversation, Run, and SSE MVP

- [ ] 用 `RunRepository` 记录 CREATED/RUNNING/COMPLETED/FAILED/CANCELLED 全部终态。
- [ ] 将内存实现替换为可切换持久化实现，保留接口级测试。
- [ ] 接入一个真实模型提供商，验证超时、空响应、模型错误和取消事件。
- [ ] 固化 SSE 事件：`conversation.started`、`run.started`、`token.delta`、`answer.completed`、`error`、`run.cancelled`。
- [ ] 增加请求 trace、模型耗时、输入/输出 token、Tool 次数和失败原因指标；日志脱敏。
- [ ] 验收：登录用户创建对话并得到稳定流式回答，其他用户无法读取或发送消息。

## Phase 3: Read-only MCP Tools

- [ ] 补齐演出搜索/详情、场次、票档、座位、评论摘要和订单查询 Tool。
- [ ] 用官方 MCP SDK 或兼容适配器替换不可验证的自定义协议部分，并保留 JSON-RPC/SSE 契约测试。
- [ ] 为每个 Tool 增加超时、有限重试、熔断、权限校验、审计和敏感字段脱敏。
- [ ] 验收：自然语言导购、场次比较和本人订单查询均无交易副作用。

## Phase 2: Knowledge and Retrieval

- [ ] 部署 PostgreSQL/PgVector、RustFS 和 Tika 解析链路。
- [ ] 实现文档版本、切块、Embedding、增量更新和失败重试。
- [ ] 实现向量、关键词、元数据、FAQ 多路召回和可解释排序。
- [ ] 将有效文档版本、片段 ID 和来源信息写入回答事件；旧版本不可作为有效政策依据。

## Phase 4: Transactional Agent

- [ ] 定义购买意图、购票草案、价格/座位快照和退款草案 DTO。
- [ ] 生成绑定用户、商品、座位、价格和过期时间的一次性确认令牌。
- [ ] 新增订单侧 Agent 专用创建接口；服务端二次校验快照、用户和令牌状态。
- [ ] 参数变化、过期、重复使用或用户变化均使确认失败；所有交易日志脱敏并可追溯。

## Phase 5: Queue and Model Governance

- [ ] 用 Redis Stream 建立模型任务队列和消费组。
- [ ] 用 Redisson 实现多实例并发许可、RPM/TPM 配额和模型路由。
- [ ] SSE 推送排队位置、开始、完成、失败和取消事件。
- [ ] 增加超时取消、租约恢复、幂等消费和积压告警；验证 Worker 异常后任务恢复。

## Phase 6: Evaluation and Production Governance

- [ ] 建立带有效版本引用的 RAGAS 数据集和回归门禁。
- [ ] 对 Prompt、Tool Schema、知识库和模型配置进行版本管理与灰度发布。
- [ ] 建立成本、延迟、幻觉、召回、Tool 调用和错误看板。
- [ ] 增加提示注入、越权、敏感信息泄露和端到端交易测试。

## Verification Commands

```powershell
mvn -pl services/agent-service -am test
mvn -pl services/agent-service -am dependency:tree
git diff --check
```

## Commit Boundaries

- `feat(agent): establish run lifecycle and storage port`
- `feat(agent): define contracts and trusted user context`
- `feat(agent): harden read-only tool and MCP boundaries`
- 后续 Phase 2–6 分别独立提交，禁止将交易能力与只读 Tool 混在同一提交。

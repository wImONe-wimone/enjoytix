## Context

`RagRolloutDecider`、授权检索服务和 Agent 的 pre-RAG/RAG 编排能力已经存在，但真实认证对话入口尚未把 rollout 决策、检索、上下文组装和响应观测串成一条运行时链路。实现必须复用现有 `AgentApplicationService`、`AgentOrchestrator`、会话/运行标识及 `AgentChatResult` 元数据，不能把 Spring AI 类型泄漏到外部接口，也不能改变工具与购买流程契约。

## Goals / Non-Goals

**Goals:**

- 在认证对话请求进入检索或上下文注入前，集中生成一次稳定的 runtime RAG decision。
- 只允许被选中的 cohort 执行授权检索和安全上下文注入；所有其他请求走兼容的 pre-RAG 路径。
- 让 rollback 具有最高优先级，并将检索异常、无命中和禁用原因转换为有限的 fallback reason。
- 在既有 conversation/run correlation identifiers 下记录 rollout、检索结果、引用覆盖率、延迟和成本观测。
- 保证工具白名单、用户身份、订单确认和幂等行为与迁移前一致。

**Non-Goals:**

- 不新增外部中间件、持久化表或新的模型/向量数据库。
- 不重新设计 `RagRolloutDecider` 的 cohort 算法、授权模型或引用协议。
- 不让非灰度请求预先检索、注入隐藏上下文或生成 RAG citations。
- 不在本变更中扩大 Agent 工具能力或改变公开响应 schema。

## Decisions

### 1. 在应用服务边界集中做一次运行时决策

由 `AgentApplicationService` 或其直接使用的 runtime policy 协调器，在每次认证请求中生成不可变 decision，包含 `ragEnabled`、`cohortKey` 和可选 `fallbackReason`。后续编排只消费该 decision，避免 Controller、检索服务和 Prompt assembler 各自重复判断。

备选方案是把 feature flag 判断分散到 retrieval service 和 prompt assembler；该方案容易导致非灰度请求仍触发检索，也难以保证 rollback 覆盖所有路径，因此不采用。

### 2. 以 decision 门控检索，以异常分类触发 pre-RAG fallback

只有 `ragEnabled` 的请求才调用 `AuthorizedKnowledgeRetrievalService` 和上下文组装器。检索异常、上下文组装异常和不可用结果在应用服务层转换成 bounded fallback reason，然后调用既有 pre-RAG 编排；fallback 不生成引用，也不把失败内容注入模型。

无命中按知识检索规范处理：保留安全的无命中结果，让既有工具、澄清或无匹配答复策略决定最终回答；不能把无命中当作可引用上下文。该方案优先保证安全降级，而不是让 RAG 故障阻断票务对话。

### 3. 复用现有结果元数据承载运行时观测

将 rollout decision、cohort key、fallback reason 和 RAG outcome 合并到现有 `AgentChatResult.ragMetadata` 或对应运行观测模型；公开回答仍只返回允许的 citations 和工具摘要。日志/指标只记录枚举、计数、耗时、token/cost 维度及 correlation identifiers，不记录原始 query、文档正文、secret 或 authorization header。

备选方案是新增独立外部观测 API 或数据库；这会扩大部署和兼容范围，当前灰度验证不需要，因此不采用。

### 4. rollback 通过现有配置在启动时读取

复用 `RagRolloutProperties` 的 rollback 配置；rollback 为每次请求的第一优先级，并在应用重启后生效。运行时不缓存正向 cohort 结果到跨请求可变状态，从而保证同一用户与 cohort key 的决策稳定且不会绕过 rollback。

## Risks / Trade-offs

- [RAG fallback 掩盖真实故障] → 记录分类 fallback reason、检索失败计数、延迟和成本，并在灰度监控中告警。
- [灰度决策与请求身份不一致] → 只使用认证后的稳定 user identity 和明确 cohort key，并添加稳定性与跨请求测试。
- [异常路径意外泄露内容或引用] → 在 fallback 分支清空 citations/context，并用安全日志断言禁止记录原始 query、文档和授权头。
- [编排改造影响购票链路] → 保持既有工具注册、订单确认、身份和幂等调用不变，并运行确认购买回归测试。
- [配置回滚需要重启] → 将 rollback 操作写入部署 runbook，先启用开关并验证 pre-RAG 指标，再按需重启实例。

## Migration Plan

1. 先补 runtime decision、fallback 分类和 Agent 主链路的失败测试，再实现最小接线。
2. 在默认关闭或零灰度配置下部署，确认所有请求均走 pre-RAG，且工具与购买回归通过。
3. 仅对内部用户开启小范围灰度，观察检索延迟、无命中/失败率、引用覆盖率、token 与成本指标。
4. 扩大 cohort 前验证同一用户决策稳定、越权隔离、提示注入防护和无命中降级。
5. 出现异常时启用 rollback 配置并重启服务；验证所有请求停止检索、上下文注入和引用发放，同时保留原有 pre-RAG 服务能力。
6. 灰度稳定后再逐步扩大比例；本变更不要求删除旧路径，pre-RAG 持续作为回滚路径保留。
## Why

EnjoyTix 已经存在独立的 `agent-service`、会话、工具、知识库和购票编排代码，但当前模型调用与工具执行主要由项目自定义抽象实现，无法充分复用 Spring AI Alibaba 的 ChatClient、Tool Calling、Agent Framework 和 Graph 能力。现在迁移可以在保留用户、票务、订单、支付等成熟领域服务的前提下，统一 AI 基础设施，降低模型适配和后续工作流演进成本。

## What Changes

- 将现有 `agent-service` 定位为项目的 AI 应用服务，不新增重复的 `ai-service` 模块。
- 将当前自定义模型调用链迁移到 Spring AI / Spring AI Alibaba 的模型、ChatClient 和工具调用抽象。
- 保留现有多模块微服务结构、领域服务、Feign 远程调用、内存模式和 MySQL/ShardingSphere 模式。
- 将现有票务查询、订单查询、用户上下文和购票能力适配为受控的 Spring AI 工具。
- 首期交付单 Agent + Tool Calling 的智能演出咨询和购票助手。
- 为有副作用的创建订单、取消订单和退款工具增加用户确认、身份校验、幂等和审计约束。
- 为后续 Spring AI Alibaba Graph Workflow 保留稳定的工具、状态和节点边界，但不在首期强制完成多 Agent 重构。
- 升级 Maven 版本治理，使 Spring Boot 3.x、Spring AI 1.1.x 和 Spring AI Alibaba 1.1.2.2 依赖保持一致。
- 增加 AI 服务、工具安全、模型不可用降级、端到端购票和现有 MVP 流程回归测试。

## Capabilities

### New Capabilities

- `ai-assistant`: 提供基于 Spring AI Alibaba 的会话、工具调用、智能演出咨询和确认式购票能力。
- `ai-tool-security`: 定义 AI 工具的身份传播、权限校验、副作用确认、幂等和审计行为。
- `ai-workflow-foundation`: 定义面向后续 Graph Workflow 的会话状态、工具结果和可观测性基础契约。

### Modified Capabilities\n\n_None._\n\n## Impact

- 构建与依赖：根 `pom.xml`、`dependencies/pom.xml`、`services/agent-service/pom.xml` 及相关 Maven 配置。
- AI 服务：`services/agent-service/src/main/java/com/wimone/enjoytix/agent` 下的模型、编排、工具、认证、知识库和 API 代码。
- 服务集成：用户、演出、票务、订单和支付远程接口及身份上下文传播。
- 配置与部署：`agent-service` 的模型供应商、DashScope/API Key、超时、重试、观测和 Nacos 配置。
- 测试：现有 agent-service 测试、`tests/mvp-flow-test`，以及新增 AI 工具和会话集成测试。
- 兼容性：首期不改变传统 REST 购票入口；AI 入口以并行方式提供，支持模型关闭或不可用时降级。


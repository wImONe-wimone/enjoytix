# Agent Compatibility POC

**Date:** 2026-09-12

## Result

Phase 0-A 的隔离 POC 已通过。POC 位于 `pocs/agent-compatibility-poc`，不加入根 Maven Reactor，避免候选依赖污染现有服务。

已验证的版本组合：

- Spring Boot `3.5.15`
- Spring Cloud `2025.0.0`
- Spring AI `1.0.9`
- MCP Java SDK `0.18.3`（由 Spring AI 传递引入）
- Sa-Token Spring Boot 3 starter `1.45.0`
- Java `17`

POC 同时引入现有 `enjoytix-web` 和 `enjoytix-distributed-id`，验证了 Agent 独立上下文、Spring AI OpenAI starter、MCP WebMVC SSE transport、Sa-Token 和现有内部模块可以共同加载。

## Verification

- Spring Boot application context 启动成功。
- `ChatClient.Builder` Bean 可用，说明 Spring AI 模型 starter 已加载。
- `WebMvcSseServerTransportProvider` 可用，说明 MCP WebMVC/SSE transport 已加载。
- `SaManager.getSaTokenContext()` 初始化成功。
- `IdGeneratorManager` Bean 可加载，确认与 `enjoytix-distributed-id` 的二进制兼容性。
- `OpenApiAutoConfiguration` 可加载，确认与 `enjoytix-web` 的二进制兼容性。
- Maven Enforcer `dependencyConvergence` 通过；显式管理 `swagger-annotations-jakarta:2.2.30` 以收敛旧版 springdoc 传递的 Swagger 依赖。

## Decision

不升级全局父工程，也不在当前 `services/agent-service` 默认依赖中直接激活 Spring AI/MCP 候选版本。当前主工程仍使用 Spring Boot `3.0.7` 和 Spring Cloud `2022.0.3`，而通过验证的 Agent 组合使用 Spring Boot `3.5.15` 和 Spring Cloud `2025.0.0`。

因此，隔离 POC 的 Phase 0-A 验收已通过，但主工程 Agent 服务的版本迁移验收尚未完成。下一步应先评估 Agent 独立依赖平台或独立 Maven 工程的落地方案，再决定是否迁移 `agent-service`；在此之前不得把 POC 依赖加入主服务默认构建。

## Commands

```powershell
mvn -pl frameworks/web,frameworks/distributed-id -am -DskipTests install
mvn -f pocs/agent-compatibility-poc/pom.xml test
mvn -f pocs/agent-compatibility-poc/pom.xml dependency:tree -Dverbose
mvn -pl services/agent-service -am test
git diff --check
```

## Gate

- [x] 隔离 Agent POC 可编译并启动上下文。
- [x] Spring AI 模型 starter 加载。
- [x] MCP Server WebMVC/SSE transport 加载。
- [x] Sa-Token Spring Boot 3 集成加载。
- [x] `enjoytix-web` 与 `enjoytix-distributed-id` 二进制兼容性验证。
- [x] 完整依赖收敛无 Spring Framework/Jackson/Netty 冲突。
- [ ] 主工程 `agent-service` 的版本迁移和生产化依赖方案。

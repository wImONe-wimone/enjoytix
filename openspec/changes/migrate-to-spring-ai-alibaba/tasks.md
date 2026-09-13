## 1. Baseline and dependency alignment

- [ ] 1.1 Record the current reactor build, agent-service tests, and MVP flow test results; verify the baseline commands and failures are documented.
- [x] 1.2 Inspect Spring Boot/Spring Cloud Alibaba compatibility and pin the selected Spring AI Alibaba 1.1.2.2 / Spring AI 1.1.2 versions in `dependencies/pom.xml`; verify Maven dependency convergence.
- [ ] 1.3 Upgrade the root and service parent Spring Boot/Cloud versions to the selected Boot 3.5.x line without changing public APIs; verify `mvn -DskipTests compile` succeeds for the full reactor.
- [x] 1.4 Add the Spring AI Alibaba model starter and required Spring AI core dependencies to `services/agent-service/pom.xml`; verify the dependency tree contains one aligned version of each Spring AI artifact.

## 2. Spring AI model and configuration adapter

- [x] 2.1 Add provider-neutral model properties, feature flags, timeouts, and tool-round limits to `services/agent-service/src/main/resources/application.yaml`; verify disabled mode starts without an API key.
- [ ] 2.2 Implement a Spring AI `ChatClient` configuration and adapter behind the existing `AgentModelClient` application boundary; verify adapter unit tests cover normal response, timeout, provider error, and round-limit behavior.
- [x] 2.3 Migrate system prompt, conversation message mapping, and response mapping without exposing provider-specific types through `AgentChatResult`; verify existing controller contract tests remain compatible.
- [ ] 2.4 Add model invocation metrics, correlation identifiers, and sanitized error classification; verify tests assert secrets and raw provider stack traces are absent from API responses and audit events.

## 3. Typed ticketing tools

- [x] 3.1 Define typed input/output contracts for performance, session, ticket availability, seat availability, current-user, and order lookup tools; verify validation tests reject missing or malformed identifiers.
- [x] 3.2 Adapt existing read-only tool classes and Feign remote services to Spring AI `@Tool`/`ToolCallback` registration; verify only the allowlisted tools are exposed to the model.
- [x] 3.3 Preserve authoritative pricing and inventory semantics by mapping tool output directly from ticket/performance services; verify tests fail when model text attempts to override price or availability.
- [x] 3.4 Enforce trusted user context propagation and reject model-supplied user identifiers; verify cross-user query tests return denial or scoped data only.

## 4. Confirmed purchase and side-effect policy

- [x] 4.1 Define quote fingerprint, expiry, confirmation, and idempotency state in the existing purchase-trade boundary; verify an unconfirmed or expired quote cannot create an order.
- [x] 4.2 Adapt order creation to a Spring AI side-effecting tool that calls the existing order remote service; verify domain validation, authoritative price, and inventory rules remain in force.
- [x] 4.3 Add duplicate-request handling for create-order, cancellation, and refund tool paths; verify the same idempotency key never creates two orders.
- [x] 4.4 Add tool audit events for subject, conversation/run, tool, outcome, latency, and failure category; verify secrets and unnecessary payment data are excluded.

## 5. Chat endpoint and workflow seam

- [x] 5.1 Switch `AgentChatController`/`AgentApplicationService` to the Spring AI Tool Calling path behind a feature flag; verify disabled flag uses the deterministic fallback and traditional APIs remain unchanged.
- [x] 5.2 Preserve conversation and run identifiers, tool summaries, and confirmation responses in the API DTOs; verify conversation continuation tests pass across multiple tool calls.
- [x] 5.3 Define an application-owned workflow state object and node-oriented tool result model compatible with future Spring AI Alibaba Graph execution; verify no Graph dependency is required for the initial path.
- [x] 5.4 Add provider smoke-test profile for DashScope using `AI_DASHSCOPE_API_KEY`; verify the default Maven test lifecycle skips it when credentials are absent.

## 6. Verification, rollout, and documentation

- [x] 6.1 Add unit and integration coverage for model outage, tool timeout, unauthorized access, stale quote, duplicate order, and inventory change races; verify all new security and failure scenarios pass.
- [x] 6.2 Run `mvn test`, the `agent-service` test suite, and `tests/mvp-flow-test`; verify the existing non-AI purchase flow remains green.
- [x] 6.3 Run packaging and startup checks for memory mode and MySQL profile; verify Nacos registration, health endpoints, and disabled-model startup.
- [x] 6.4 Document environment variables, feature-flag rollback, provider configuration, tool safety rules, and the future Graph migration boundary in `README.md` and deployment configuration; verify a new developer can start the service without provider credentials.
- [x] 6.5 Perform a final dependency/license/security review and record the rollout gate; verify `openspec validate --change migrate-to-spring-ai-alibaba` passes.

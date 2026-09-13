## Context

See `proposal.md` for motivation and capability deltas. The repository already contains `services/agent-service`, custom model/client abstractions, tool registries, Feign ports, authentication propagation, knowledge ingestion, and purchase-draft services. The migration therefore replaces the AI integration seam in that module instead of creating a second AI service.

The selected baseline is Spring Boot 3.5.x with Spring AI 1.1.2 and Spring AI Alibaba 1.1.2.2. The Spring AI Alibaba 2.0.0-M1.1 / Spring Boot 4 line is outside this change because it is a separate milestone migration.

## Goals / Non-Goals

**Goals:**

- Make `agent-service` use Spring AI Alibaba model and agent abstractions.
- Preserve domain-service ownership, Feign boundaries, authentication propagation, and traditional REST behavior.
- Deliver single-agent Tool Calling for show discovery, availability, order lookup, and confirmed purchase.
- Keep application-owned conversation/run DTOs and tool policy seams stable for later Graph Workflow adoption.
- Provide deterministic behavior when the model is disabled or unavailable.

**Non-Goals:**

- Do not rewrite user, performance, ticket, order, pay, or gateway services as agents.
- Do not introduce multi-agent orchestration, Graph runtime execution, A2A, MCP replacement, or a vector database in the first increment.
- Do not let the model access repositories or databases directly.
- Do not change non-AI order, payment, inventory, or refund contracts.

## Decisions

### Reuse `agent-service`

The repository already has the service and most domain adapters. Renaming or adding a parallel `ai-service` would create duplicate deployment and configuration paths. Keep the module and migrate internals incrementally.

### Centralize versions

Add Spring AI Alibaba BOM and aligned Spring AI model starter dependencies in dependency management. Pin versions in one place and upgrade Spring Boot/Cloud only as required by the compatibility matrix.

### Use `ChatClient` and typed tools

Adapt existing tool implementations to typed Spring AI `@Tool` methods or `ToolCallback` instances. A policy-aware resolver preserves allowlisting, audit, timeout, retry, and error sanitization.

### Keep side effects behind policy gates

Read tools require authentication and authorization. Purchase, cancellation, and refund require a current quote, explicit confirmation, idempotency key, and domain validation. The order service remains authoritative.

### Preserve application-owned state

Keep existing conversation/run models as the external contract. Provider-specific messages remain behind an adapter so the same identifiers can later map to Graph state.

### Introduce Graph incrementally

The first path is ChatClient plus Tool Calling. Tool results, confirmation state, correlation identifiers, and failure categories become future graph state fields. Graph nodes are a follow-up, not a first-slice prerequisite.

### Provider-independent tests first

Unit tests use mocked model/client boundaries and Feign ports. Provider smoke tests are profile-gated, require `AI_DASHSCOPE_API_KEY`, and are never required by the default build.

## Risks / Trade-offs

- [Spring Boot 3.5 upgrade affects Cloud modules] → Verify the compatibility matrix and run the full Maven reactor before behavior migration.
- [AI APIs vary by patch release] → Pin `1.1.2.2` and isolate framework calls behind application adapters.
- [Model arguments cross user boundaries] → Derive identity only from trusted request context and reject model-supplied identity.
- [Stale quotes create incorrect orders] → Store quote fingerprint/expiry, refresh authoritative data before creation, and require idempotency.
- [Model outage makes the assistant unreliable] → Bound timeouts/tool rounds, return stable error codes, and perform no mutation on failed model execution.

## Migration Plan

1. Capture Maven/test baseline and dependency graph.
2. Align Spring Boot/Spring AI Alibaba dependencies without changing public API behavior.
3. Add provider configuration and a Spring AI model adapter behind the current application service.
4. Adapt read-only tools and security/audit policy.
5. Adapt quote and confirmed order creation with stale-quote and idempotency checks.
6. Switch the chat endpoint behind a feature flag while retaining rollback path.
7. Run module, reactor, MVP, security, and failure-mode tests.
8. Enable the new path by environment configuration after observability is confirmed.

Rollback is configuration-first: disable the feature flag/model path and keep traditional REST purchase active. If dependency alignment blocks the reactor, revert the dependency-platform change before continuing.

## Open Questions

None that change scope or task breakdown. The DashScope deployment/model name remains an environment-level choice implementing the selected Spring AI `ChatModel` contract.

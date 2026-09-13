# Agent Spring AI Alibaba Migration

The agent service keeps `AgentModelClient` as its application boundary and uses
`SpringAiAlibabaAgentModelClient` for DashScope tool calling. Provider-specific
Spring AI messages and options stay inside the adapter; the orchestrator still
owns tool execution, round limits, and the public chat result.

## Configuration

- `agent.model.enabled=false` keeps the deterministic fallback and does not
  require provider credentials.
- `agent.model.enabled=true` with `agent.model.provider=dashscope` enables the
  DashScope adapter.
- `AI_DASHSCOPE_API_KEY` supplies the DashScope credential.
- `AGENT_MODEL_NAME` selects the DashScope chat model and defaults to
  `qwen-plus`.
- `AGENT_DASHSCOPE_AGENT_ENABLED` is intentionally false by default; the
  service performs tool rounds through its own allowlisted registry rather than
  delegating execution to the provider agent.

The current Spring Cloud Alibaba release train remains in place for existing
Nacos and Feign clients. Boot 3.5 compatibility is guarded by a local
`factoryBeanObjectType` metadata normalizer and the Cloud compatibility verifier
is disabled until the Cloud Alibaba train is upgraded together.

## Workflow Boundary

The initial implementation is a single agent plus tool-calling loop. Purchase
confirmation, quote expiry, idempotency, and audit policy remain application
owned. A future Graph workflow can replace `AgentOrchestrator` without exposing
Spring AI types to controllers or domain services.

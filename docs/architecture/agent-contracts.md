# Agent API Contracts

## API boundaries

| Boundary | Prefix | Caller | Allowed behavior |
| --- | --- | --- | --- |
| Public API | `/api/agent/**` | Login user through Gateway | Conversation creation, owned conversation reads, SSE answer streaming |
| Internal API | `/internal/agent/**` | Trusted service-to-service callers | Explicitly allow-listed read-only queries; never exposed through public Gateway routes |
| Tool API | `/mcp/**` | MCP client or Agent runtime | Tool discovery and read-only execution; every call is authenticated and audited |

## User context

The Gateway authenticates the bearer token and forwards the resolved identity as trusted request metadata. The Agent service resolves that metadata through `AgentAuthenticationPort`; controllers must not parse user headers themselves. The current header adapter is a compatibility bridge for the existing Gateway contract, not a replacement for Sa-Token validation.

The Sa-Token implementation must replace or wrap `HeaderAgentAuthenticationAdapter` before the Agent service is reachable without the Gateway. Direct service calls must require a service credential and must not accept arbitrary client-supplied `X-User-Id` values.

## DTO rules

- Public request/response DTOs must not expose persistence records directly.
- Internal DTOs contain only fields required by the consuming service and use explicit nullable semantics.
- Tool results must identify the source service and query timestamp, and must omit credentials, payment data, and unrelated user fields.
- Transaction DTOs are not valid inputs to read-only Tool APIs.

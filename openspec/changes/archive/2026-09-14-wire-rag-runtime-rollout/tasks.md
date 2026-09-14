## 1. Runtime Decision

- [x] 1.1 Define immutable runtime RAG decision with bounded fallback reasons.
- [x] 1.2 Add rollout policy with disabled, outside-cohort, and rollback precedence.
- [x] 1.3 Classify no-hit, retrieval failure, and context assembly fallback outcomes.

## 2. Agent Integration

- [x] 2.1 Gate retrieval and context injection from the authenticated assistant request path.
- [x] 2.2 Preserve the pre-RAG path for disabled, non-cohort, no-hit, failure, and rollback cases.
- [x] 2.3 Inject only authorized retrieved context as untrusted evidence.
- [x] 2.4 Return citations only for successful retrieved knowledge.
- [x] 2.5 Merge rollout, cohort, fallback, and retrieval metadata into the chat result.

## 3. Observability and Security

- [x] 3.1 Record retrieval outcome, counts, citation coverage, and latency dimensions.
- [x] 3.2 Keep query text, document bodies, secrets, and authorization headers out of observations.
- [x] 3.3 Add production metrics wiring for model latency, token usage, and estimated cost.

## 4. Regression Verification

- [x] 4.1 Pass the agent-service test suite.
- [x] 4.2 Verify the full Maven reactor and confirmed purchase flow.
- [x] 4.3 Verify RAG enabled, no-hit, retrieval failure, non-cohort, and rollback scenarios end to end.
- [x] 4.4 Pass reactor compilation without tests.
- [x] 4.5 Pass OpenSpec status and strict validation.

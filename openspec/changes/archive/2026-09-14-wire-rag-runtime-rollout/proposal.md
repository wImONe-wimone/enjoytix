## Why

The RAG components and rollout decider are implemented, but the decider is not yet part of the real assistant request path. This creates a gap between configured canary/rollback policy and runtime behavior: operators cannot rely on the rollout flag to guarantee that selected users receive RAG or that rollback immediately restores the pre-RAG flow.

## What Changes

- Route every authenticated assistant request through one runtime RAG decision before retrieval or context injection.
- Apply stable internal-user and canary cohort selection using the existing rollout configuration.
- Preserve the existing non-RAG assistant path for disabled, non-cohort, no-hit, and retrieval-failure cases.
- Make explicit rollback disable RAG retrieval, context injection, and citation emission without changing ticket tools or purchase confirmation policy.
- Record rollout decision, cohort, fallback reason, and RAG outcome under the existing conversation/run correlation identifiers.
- Add unit, service, and end-to-end flow tests for enabled, disabled, canary, rollback, no-hit, retrieval failure, and confirmed purchase compatibility.

## Capabilities

### New Capabilities

<!-- None. This change wires behavior into existing capabilities. -->

### Modified Capabilities

- `ai-assistant`: assistant requests select the RAG or pre-RAG path at runtime and safely fall back without changing tool and purchase contracts.
- `ai-workflow-foundation`: workflow runs expose the rollout decision and fallback reason alongside retrieval observability.
- `knowledge-retrieval`: configured rollout and rollback controls govern whether retrieval/context/citations are attempted for a request.

## Impact

- Affects `services/agent-service` request orchestration, RAG retrieval integration, response metadata, metrics/audit fields, and tests.
- Uses the existing `RagRolloutProperties` and `RagRolloutDecider`; no new external dependency or database migration is required.
- The pre-RAG ChatClient/tool flow remains the compatibility path and remains available when RAG is disabled or unavailable.
# Spring AI Alibaba Rollout Gate

**Review date:** 2026-09-13

## Status: HOLD

The migration is buildable and its tested safe paths pass, but it is not approved
for production rollout until the blocking conditions below are closed or formally
accepted by the release owner.

## Evidence reviewed

| Area | Result | Evidence |
| --- | --- | --- |
| Dependency alignment | Pass with release risk | `spring-ai-alibaba-starter-dashscope:1.1.2.2` resolves Spring AI artifacts at `1.1.2`; the selected Spring AI Alibaba and Spring AI POM metadata declares Apache-2.0. |
| Nacos dependencies | Pass with operational verification required | The selected Spring Cloud Alibaba/Nacos train is the GA release `2025.0.0.0`; service registration must still be verified against the target Nacos environment before rollout. |
| Source and configuration secrets | Pass | `git diff --check` passes; a high-confidence tracked-file secret scan finds no private keys or known API-key formats. Agent credentials remain empty environment-variable placeholders. |
| Safe defaults | Pass | Model and tool calling are disabled by default; the credential-free Agent JAR health check passes with Nacos disabled. |
| Regression | Pass | `mvn -pl services/agent-service -am test` and `mvn -pl tests/mvp-flow-test -am test` pass on the review date. |
| Dependency hygiene | Follow-up required | `mvn -pl services/agent-service dependency:analyze` reports used-undeclared and unused-declared dependencies; review and explicitly declare or waive each before release. |
| Vulnerability scanning | Blocking risk | No `osv-scanner`, `trivy`, `grype`, or `syft` executable is installed locally, and this repository has no configured automated CVE/SBOM gate. No assertion of zero known vulnerabilities is made. |

## License review

The Maven POM metadata for the new Spring AI Alibaba DashScope starter and core,
Spring AI model artifact, and Nacos discovery/config starters declares Apache-2.0.
This review covers the migration's selected direct integration artifacts only; the
production gate must generate and retain a complete transitive SBOM and license
report.

## Required actions before production

1. Add a CI vulnerability and SBOM scan using a maintained advisory database; fail
   release on unapproved high- or critical-severity findings and archive its report.
2. Resolve or explicitly waive each `dependency:analyze` finding with the service
   owner, avoiding accidental reliance on transitive dependencies.
3. Verify Nacos registration and configuration loading against the target environment
   using the approved GA train, then retain the startup and health-check evidence.
4. Keep `AGENT_MODEL_ENABLED=false` until DashScope credentials, observability,
   tool auditing, and the feature-flag rollback procedure have been reviewed in
   the target environment.

## Release-owner decision

**Current decision:** HOLD. Do not enable the DashScope provider or deploy this
change to production until every required action is complete or has an approved,
time-bounded exception.

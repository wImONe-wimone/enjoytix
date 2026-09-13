package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryAgentToolExecutorTest {
    @org.junit.jupiter.api.AfterEach
    void clearContext() {
        AgentUserContextHolder.clear();
    }
    @Test
    void retriesTransientFailureAndAuditsAttemptCount() {
        AtomicInteger invocations = new AtomicInteger();
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool(() -> invocations.incrementAndGet() < 2
                ? AgentToolResult.failure("TOOL_EXECUTION_FAILED", "failed")
                : AgentToolResult.success(Map.of("ok", true))));
        AgentToolExecutionProperties properties = properties(1, 3, Duration.ofSeconds(1));
        AtomicReferenceAuditSink audit = new AtomicReferenceAuditSink();

        AgentToolResult result = new RegistryAgentToolExecutor(registry, new SimpleMeterRegistry(), properties, audit)
                .execute("stub", new AgentToolRequest(Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(invocations).hasValue(2);
        assertThat(audit.event.attempts()).isEqualTo(2);
    }

    @Test
    void timesOutAndOpensCircuitAfterThreshold() {
        AtomicInteger invocations = new AtomicInteger();
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool(() -> {
            invocations.incrementAndGet();
            try { Thread.sleep(100); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            return AgentToolResult.success(Map.of());
        }));
        AgentToolExecutionProperties properties = properties(0, 1, Duration.ofMillis(10));
        RegistryAgentToolExecutor executor = new RegistryAgentToolExecutor(registry, new SimpleMeterRegistry(), properties,
                new AtomicReferenceAuditSink());

        assertThat(executor.execute("stub", new AgentToolRequest(Map.of())).errorCode()).isEqualTo("TOOL_TIMEOUT");
        assertThat(executor.execute("stub", new AgentToolRequest(Map.of())).errorCode()).isEqualTo("TOOL_CIRCUIT_OPEN");
        assertThat(invocations).hasValue(1);
    }

    @Test
    void propagatesAuthenticatedContextIntoAsyncToolExecution() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new AgentTool() {
            @Override public String name() { return "context"; }
            @Override public String description() { return "context"; }
            @Override public Map<String, Object> inputSchema() { return Map.of("type", "object"); }
            @Override public AgentToolResult execute(AgentToolRequest request) {
                return AgentToolResult.success(AgentUserContextHolder.requireCurrent().userId());
            }
        });
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));

        AgentToolResult result = new RegistryAgentToolExecutor(registry, new SimpleMeterRegistry(),
                properties(0, 3, Duration.ofSeconds(1)), new AtomicReferenceAuditSink())
                .execute("context", new AgentToolRequest(Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(7L);
    }

    @Test
    void recordsSafeAuditMetadataForAuthenticatedToolInvocation() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new NamedStubTool("create_order", () -> AgentToolResult.success(Map.of(
                "orderId", "order-1",
                "paymentToken", "secret-payment-token"))));
        AtomicReferenceAuditSink audit = new AtomicReferenceAuditSink();
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));

        AgentToolResult result = new RegistryAgentToolExecutor(registry, new SimpleMeterRegistry(),
                properties(0, 3, Duration.ofSeconds(1)), audit)
                .execute("create_order", new AgentToolRequest(Map.of(
                        "confirmationToken", "secret-confirmation-token",
                        "idempotencyKey", "secret-idempotency-key")),
                        new AgentToolExecutionContext("conversation-1", "run-1"));

        assertThat(result.success()).isTrue();
        assertThat(audit.event.subject()).isEqualTo("user:7");
        assertThat(audit.event.conversationId()).isEqualTo("conversation-1");
        assertThat(audit.event.runId()).isEqualTo("run-1");
        assertThat(audit.event.toolName()).isEqualTo("create_order");
        assertThat(audit.event.outcome()).isEqualTo("SUCCESS");
        assertThat(audit.event.failureCategory()).isNull();
        assertThat(audit.event.durationMillis()).isGreaterThanOrEqualTo(0);
        assertThat(audit.event.toString()).doesNotContain("confirmationToken", "idempotencyKey",
                "secret-confirmation-token", "secret-idempotency-key", "paymentToken", "secret-payment-token");
    }

    @Test
    void recordsFailureCategoryAndAnonymousSubjectWithoutArguments() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new NamedStubTool("lookup", () -> AgentToolResult.failure("TOOL_TIMEOUT", "timed out")));
        AtomicReferenceAuditSink audit = new AtomicReferenceAuditSink();

        new RegistryAgentToolExecutor(registry, new SimpleMeterRegistry(),
                properties(0, 3, Duration.ofSeconds(1)), audit)
                .execute("lookup", new AgentToolRequest(Map.of("apiKey", "secret-api-key")));

        assertThat(audit.event.subject()).isEqualTo("anonymous");
        assertThat(audit.event.conversationId()).isNull();
        assertThat(audit.event.runId()).isNotBlank();
        assertThat(UUID.fromString(audit.event.runId())).isNotNull();
        assertThat(audit.event.outcome()).isEqualTo("FAILURE");
        assertThat(audit.event.failureCategory()).isEqualTo("TIMEOUT");
        assertThat(audit.event.toString()).doesNotContain("apiKey", "secret-api-key");
    }

    private AgentToolExecutionProperties properties(int retries, int threshold, Duration timeout) {
        AgentToolExecutionProperties properties = new AgentToolExecutionProperties();
        properties.setMaxRetries(retries);
        properties.setFailureThreshold(threshold);
        properties.setTimeout(timeout);
        properties.setOpenDuration(Duration.ofSeconds(1));
        return properties;
    }

    private record StubTool(java.util.function.Supplier<AgentToolResult> supplier) implements AgentTool {
        @Override public String name() { return "stub"; }
        @Override public String description() { return "stub"; }
        @Override public Map<String, Object> inputSchema() { return Map.of("type", "object"); }
        @Override public AgentToolResult execute(AgentToolRequest request) { return supplier.get(); }
    }

    private record NamedStubTool(String toolName, java.util.function.Supplier<AgentToolResult> supplier) implements AgentTool {
        @Override public String name() { return toolName; }
        @Override public String description() { return toolName; }
        @Override public Map<String, Object> inputSchema() { return Map.of("type", "object"); }
        @Override public AgentToolResult execute(AgentToolRequest request) { return supplier.get(); }
    }

    private static class AtomicReferenceAuditSink implements AgentToolAuditSink {
        private AgentToolAuditEvent event;
        @Override public void record(AgentToolAuditEvent event) { this.event = event; }
    }
}

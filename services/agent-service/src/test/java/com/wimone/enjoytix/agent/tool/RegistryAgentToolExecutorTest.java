package com.wimone.enjoytix.agent.tool;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryAgentToolExecutorTest {
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

    private static class AtomicReferenceAuditSink implements AgentToolAuditSink {
        private AgentToolAuditEvent event;
        @Override public void record(AgentToolAuditEvent event) { this.event = event; }
    }
}

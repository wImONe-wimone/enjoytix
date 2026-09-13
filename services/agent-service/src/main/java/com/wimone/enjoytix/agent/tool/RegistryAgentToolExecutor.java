package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.UUID;

@Component
public class RegistryAgentToolExecutor implements AgentToolExecutor {
    private final AgentToolRegistry registry;
    private final MeterRegistry meters;
    private final AgentToolExecutionProperties properties;
    private final AgentToolAuditSink auditSink;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "agent-tool-executor");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public RegistryAgentToolExecutor(AgentToolRegistry registry, MeterRegistry meters,
                                     AgentToolExecutionProperties properties, AgentToolAuditSink auditSink) {
        this.registry = registry;
        this.meters = meters;
        this.properties = properties;
        this.auditSink = auditSink;
    }

    @Override
    public AgentToolResult execute(String toolName, AgentToolRequest request) {
        return execute(toolName, request, new AgentToolExecutionContext(null, UUID.randomUUID().toString()));
    }

    @Override
    public AgentToolResult execute(String toolName, AgentToolRequest request, AgentToolExecutionContext executionContext) {
        if (executionContext == null) executionContext = new AgentToolExecutionContext(null, UUID.randomUUID().toString());
        long started = System.nanoTime();
        int attempts = 0;
        if (isOpen(toolName)) {
            return finish(toolName, AgentToolResult.failure("TOOL_CIRCUIT_OPEN", "Agent tool temporarily unavailable"), started, attempts, executionContext);
        }
        AgentToolResult result;
        do {
            attempts++;
            result = invokeWithTimeout(toolName, request);
        } while (isRetryable(result) && attempts <= properties.getMaxRetries());
        if (isRetryable(result)) recordFailure(toolName); else recordSuccess(toolName);
        return finish(toolName, result, started, attempts, executionContext);
    }

    private AgentToolResult invokeWithTimeout(String toolName, AgentToolRequest request) {
        try {
            var userContext = AgentUserContextHolder.current();
            return CompletableFuture.supplyAsync(() -> {
                        final AgentToolResult[] result = new AgentToolResult[1];
                        Runnable action = () -> result[0] = registry.find(toolName)
                                .map(tool -> tool.execute(request))
                                .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", "Unknown agent tool: " + toolName));
                        if (userContext == null) {
                            action.run();
                        } else {
                            AgentUserContextHolder.runAs(userContext, action);
                        }
                        return result[0];
                    }, executor)
                    .orTimeout(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof TimeoutException) return AgentToolResult.failure("TOOL_TIMEOUT", "Agent tool timed out");
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Agent tool execution failed");
        }
    }

    private boolean isRetryable(AgentToolResult result) {
        return !result.success() && ("TOOL_TIMEOUT".equals(result.errorCode()) || "TOOL_EXECUTION_FAILED".equals(result.errorCode()));
    }

    private boolean isOpen(String toolName) {
        CircuitState state = circuits.get(toolName);
        return state != null && state.isOpen(properties.getOpenDuration());
    }

    private void recordFailure(String toolName) {
        circuits.computeIfAbsent(toolName, ignored -> new CircuitState()).failure(properties.getFailureThreshold());
    }

    private void recordSuccess(String toolName) {
        circuits.remove(toolName);
    }

    private AgentToolResult finish(String toolName, AgentToolResult result, long started, int attempts, AgentToolExecutionContext executionContext) {
        meters.counter("enjoytix.agent.tool.calls", "tool", toolName,
                "status", result.success() ? "success" : "failure").increment();
        meters.timer("enjoytix.agent.tool.duration", "tool", toolName)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        var context = AgentUserContextHolder.current();
        auditSink.record(new AgentToolAuditEvent(toolName, context == null ? "anonymous" : "user:" + context.userId(),
                executionContext.conversationId(), executionContext.runId(), result.success() ? "SUCCESS" : "FAILURE",
                failureCategory(result.errorCode()), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), attempts));
        return result;
    }

    private String failureCategory(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) return null;
        if ("TOOL_TIMEOUT".equals(errorCode)) return "TIMEOUT";
        if ("TOOL_EXECUTION_FAILED".equals(errorCode)) return "EXECUTION";
        if ("TOOL_CIRCUIT_OPEN".equals(errorCode)) return "CIRCUIT_OPEN";
        if (errorCode.startsWith("AUTH") || errorCode.contains("UNAUTHORIZED")) return "AUTHORIZATION";
        if (errorCode.startsWith("VALIDATION") || errorCode.contains("INVALID")) return "VALIDATION";
        return "DOMAIN";
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class CircuitState {
        private int failures;
        private long openedAt;

        synchronized void failure(int threshold) {
            failures++;
            if (failures >= Math.max(1, threshold)) openedAt = System.nanoTime();
        }

        synchronized boolean isOpen(java.time.Duration duration) {
            return openedAt != 0 && System.nanoTime() - openedAt < duration.toNanos();
        }
    }
}

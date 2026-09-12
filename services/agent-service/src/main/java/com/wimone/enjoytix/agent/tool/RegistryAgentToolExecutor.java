package com.wimone.enjoytix.agent.tool;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RegistryAgentToolExecutor implements AgentToolExecutor {
    private final AgentToolRegistry registry;
    private final MeterRegistry meters;

    public RegistryAgentToolExecutor(AgentToolRegistry registry, MeterRegistry meters) {
        this.registry = registry;
        this.meters = meters;
    }

    @Override
    public AgentToolResult execute(String toolName, AgentToolRequest request) {
        long started = System.nanoTime();
        try {
            AgentToolResult result = registry.find(toolName)
                    .map(tool -> tool.execute(request))
                    .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", "Unknown agent tool: " + toolName));
            meters.counter("enjoytix.agent.tool.calls", "tool", toolName,
                    "status", result.success() ? "success" : "failure").increment();
            return result;
        } catch (RuntimeException ex) {
            meters.counter("enjoytix.agent.tool.calls", "tool", toolName, "status", "failure").increment();
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Agent tool execution failed");
        } finally {
            meters.timer("enjoytix.agent.tool.duration", "tool", toolName)
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }
}

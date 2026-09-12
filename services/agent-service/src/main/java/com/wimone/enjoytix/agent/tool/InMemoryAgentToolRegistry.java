package com.wimone.enjoytix.agent.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemoryAgentToolRegistry implements AgentToolRegistry {
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    @Override
    public synchronized void register(AgentTool tool) {
        if (tool == null || tool.name() == null || tool.name().isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank");
        }
        if (tools.containsKey(tool.name())) {
            throw new IllegalArgumentException("Tool already registered: " + tool.name());
        }
        tools.put(tool.name(), tool);
    }

    @Override
    public synchronized Optional<AgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    public synchronized List<AgentTool> list() {
        return List.copyOf(new ArrayList<>(tools.values()));
    }
}
package com.wimone.enjoytix.agent.tool;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class AgentToolRegistrar implements SmartInitializingSingleton {
    private final AgentToolRegistry registry;
    private final ObjectProvider<AgentTool> tools;

    public AgentToolRegistrar(AgentToolRegistry registry, ObjectProvider<AgentTool> tools) {
        this.registry = registry;
        this.tools = tools;
    }

    @Override
    public void afterSingletonsInstantiated() {
        tools.orderedStream().forEach(registry::register);
    }
}
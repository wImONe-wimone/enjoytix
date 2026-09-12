package com.wimone.enjoytix.agent.tool;

import java.util.List;
import java.util.Optional;

public interface AgentToolRegistry {
    void register(AgentTool tool);

    Optional<AgentTool> find(String name);

    List<AgentTool> list();
}
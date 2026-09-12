package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentRun;

public interface RunRepository {
    AgentRun save(AgentRun run);

    AgentRun find(Long runId);
}

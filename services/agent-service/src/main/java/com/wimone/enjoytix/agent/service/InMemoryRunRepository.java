package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentRun;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryRunRepository implements RunRepository {
    private final Map<Long, AgentRun> runs = new ConcurrentHashMap<>();

    @Override
    public AgentRun save(AgentRun run) {
        runs.put(run.runId(), run);
        return run;
    }

    @Override
    public AgentRun find(Long runId) {
        return runs.get(runId);
    }
}

package com.wimone.enjoytix.agent.tool;

public interface AgentToolAuditSink {
    void record(AgentToolAuditEvent event);
}

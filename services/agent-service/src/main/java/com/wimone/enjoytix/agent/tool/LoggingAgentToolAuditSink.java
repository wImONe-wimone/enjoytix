package com.wimone.enjoytix.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingAgentToolAuditSink implements AgentToolAuditSink {
    private static final Logger log = LoggerFactory.getLogger(LoggingAgentToolAuditSink.class);

    @Override
    public void record(AgentToolAuditEvent event) {
        log.info(event.toString());
    }
}

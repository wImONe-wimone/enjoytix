package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentStreamEvent;

public interface AgentStreamListener {
    void onEvent(AgentStreamEvent event);

    default boolean isCancelled() {
        return false;
    }
}

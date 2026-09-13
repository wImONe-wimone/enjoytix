package com.wimone.enjoytix.agent.service;

import java.util.concurrent.TimeoutException;

public class AgentModelErrorClassifier {

    public AgentModelFailure classify(Throwable throwable) {
        if (hasCause(throwable, TimeoutException.class)) {
            return new AgentModelFailure(AgentModelFailureCategory.TIMEOUT,
                    "The assistant request timed out. Please try again.");
        }
        if (hasRoundLimit(throwable)) {
            return new AgentModelFailure(AgentModelFailureCategory.ROUND_LIMIT,
                    "The assistant could not complete the request. Please refine your question.");
        }
        return new AgentModelFailure(AgentModelFailureCategory.PROVIDER_ERROR,
                "The assistant is temporarily unavailable. Please try again.");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (expectedType.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRoundLimit(Throwable throwable) {
        return throwable instanceof AgentModelException
                && throwable.getMessage() != null
                && throwable.getMessage().contains("maximum tool rounds");
    }
}

package com.wimone.enjoytix.agent.auth;

public final class AgentUserContextHolder {
    private static final ThreadLocal<AgentUserContext> CURRENT = new ThreadLocal<>();

    private AgentUserContextHolder() {
    }

    public static AgentUserContext current() {
        return CURRENT.get();
    }

    public static AgentUserContext requireCurrent() {
        AgentUserContext context = current();
        if (context == null) {
            throw new AgentAuthenticationException("Authenticated user context is required");
        }
        return context;
    }

    public static void set(AgentUserContext context) {
        CURRENT.set(context);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void runAs(AgentUserContext context, Runnable action) {
        AgentUserContext previous = CURRENT.get();
        try {
            CURRENT.set(context);
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

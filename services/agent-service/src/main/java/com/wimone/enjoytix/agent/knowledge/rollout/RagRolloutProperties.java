package com.wimone.enjoytix.agent.knowledge.rollout;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.knowledge.rag.rollout")
public class RagRolloutProperties {
    private boolean enabled;
    private int canaryPercentage;
    private String internalUserIds = "";
    private boolean rollback;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCanaryPercentage() {
        return canaryPercentage;
    }

    public void setCanaryPercentage(int canaryPercentage) {
        if (canaryPercentage < 0 || canaryPercentage > 100) {
            throw new IllegalArgumentException("canaryPercentage must be between 0 and 100");
        }
        this.canaryPercentage = canaryPercentage;
    }

    public String getInternalUserIds() {
        return internalUserIds;
    }

    public void setInternalUserIds(String internalUserIds) {
        this.internalUserIds = internalUserIds == null ? "" : internalUserIds;
    }

    public boolean isRollback() {
        return rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }
}
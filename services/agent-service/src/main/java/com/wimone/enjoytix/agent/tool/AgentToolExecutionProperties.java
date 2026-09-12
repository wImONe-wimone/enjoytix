package com.wimone.enjoytix.agent.tool;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.tool")
public class AgentToolExecutionProperties {
    private Duration timeout = Duration.ofSeconds(5);
    private int maxRetries = 1;
    private int failureThreshold = 3;
    private Duration openDuration = Duration.ofSeconds(30);

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
    public Duration getOpenDuration() { return openDuration; }
    public void setOpenDuration(Duration openDuration) { this.openDuration = openDuration; }
}

package com.wimone.enjoytix.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.timeout-message")
public class OrderTimeoutMessageProperties {

    private String topic = "enjoytix_order_timeout";
    private String consumerGroup = "enjoytix_order_timeout_close_cg";
    private long sendTimeoutMillis = 3000L;
    private int maxReconsumeTimes = 5;
    private int compensationBatchSize = 100;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public long getSendTimeoutMillis() {
        return sendTimeoutMillis;
    }

    public void setSendTimeoutMillis(long sendTimeoutMillis) {
        this.sendTimeoutMillis = sendTimeoutMillis;
    }

    public int getMaxReconsumeTimes() {
        return maxReconsumeTimes;
    }

    public void setMaxReconsumeTimes(int maxReconsumeTimes) {
        this.maxReconsumeTimes = maxReconsumeTimes;
    }

    public int getCompensationBatchSize() {
        return compensationBatchSize;
    }

    public void setCompensationBatchSize(int compensationBatchSize) {
        this.compensationBatchSize = compensationBatchSize;
    }
}

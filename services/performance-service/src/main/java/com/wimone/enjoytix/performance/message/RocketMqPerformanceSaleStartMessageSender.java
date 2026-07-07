package com.wimone.enjoytix.performance.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.performance.config.PerformanceSaleMessageProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Profile("!test")
public class RocketMqPerformanceSaleStartMessageSender implements PerformanceSaleStartMessageSender {

    private static final Logger log = LoggerFactory.getLogger(RocketMqPerformanceSaleStartMessageSender.class);
    private static final long[] DELAY_LEVEL_SECONDS = {
            1L, 5L, 10L, 30L, 60L, 120L, 180L, 240L, 300L,
            360L, 420L, 480L, 540L, 600L, 1200L, 1800L, 3600L, 7200L
    };

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final PerformanceSaleMessageProperties properties;

    public RocketMqPerformanceSaleStartMessageSender(
            RocketMQTemplate rocketMQTemplate,
            ObjectMapper objectMapper,
            PerformanceSaleMessageProperties properties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void send(PerformanceSaleStartMessage saleStartMessage) {
        try {
            Message<String> message = MessageBuilder
                    .withPayload(objectMapper.writeValueAsString(saleStartMessage))
                    .setHeader(RocketMQHeaders.KEYS, saleStartMessage.messageKey())
                    .build();
            rocketMQTemplate.syncSend(
                    properties.getTopic(),
                    message,
                    properties.getSendTimeoutMillis(),
                    delayLevel(saleStartMessage.scheduledSaleTime())
            );
        } catch (JsonProcessingException ex) {
            log.error("Serialize performance sale start message failed, performanceId={}", saleStartMessage.performanceId(), ex);
            throw new IllegalStateException("Serialize performance sale start message failed", ex);
        }
    }

    private int delayLevel(LocalDateTime saleStartTime) {
        long delaySeconds = Math.max(1L, Duration.between(LocalDateTime.now(), saleStartTime).getSeconds());
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (delaySeconds <= DELAY_LEVEL_SECONDS[i]) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_SECONDS.length;
    }
}

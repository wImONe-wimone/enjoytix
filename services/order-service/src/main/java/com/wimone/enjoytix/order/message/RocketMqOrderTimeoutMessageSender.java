package com.wimone.enjoytix.order.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.order.config.OrderTimeoutMessageProperties;
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
public class RocketMqOrderTimeoutMessageSender implements OrderTimeoutMessageSender {

    private static final Logger log = LoggerFactory.getLogger(RocketMqOrderTimeoutMessageSender.class);
    private static final long[] DELAY_LEVEL_SECONDS = {
            1L, 5L, 10L, 30L, 60L, 120L, 180L, 240L, 300L,
            360L, 420L, 480L, 540L, 600L, 1200L, 1800L, 3600L, 7200L
    };

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final OrderTimeoutMessageProperties properties;
    private final OrderTimeoutMessageLogService logService;

    public RocketMqOrderTimeoutMessageSender(
            RocketMQTemplate rocketMQTemplate,
            ObjectMapper objectMapper,
            OrderTimeoutMessageProperties properties,
            OrderTimeoutMessageLogService logService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.logService = logService;
    }

    @Override
    public void send(OrderTimeoutMessage orderTimeoutMessage) {
        logService.recordSent(orderTimeoutMessage);
        try {
            Message<String> message = MessageBuilder
                    .withPayload(objectMapper.writeValueAsString(orderTimeoutMessage))
                    .setHeader(RocketMQHeaders.KEYS, orderTimeoutMessage.messageKey())
                    .build();
            rocketMQTemplate.syncSend(
                    properties.getTopic(),
                    message,
                    properties.getSendTimeoutMillis(),
                    delayLevel(orderTimeoutMessage.expireTime())
            );
        } catch (JsonProcessingException ex) {
            logService.recordSendFailed(orderTimeoutMessage, ex);
            log.error("Serialize order timeout message failed, orderId={}", orderTimeoutMessage.orderId(), ex);
        } catch (RuntimeException ex) {
            logService.recordSendFailed(orderTimeoutMessage, ex);
            log.error("Send order timeout message failed, orderId={}", orderTimeoutMessage.orderId(), ex);
        }
    }

    private int delayLevel(LocalDateTime expireTime) {
        long delaySeconds = Math.max(1L, Duration.between(LocalDateTime.now(), expireTime).getSeconds());
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (delaySeconds <= DELAY_LEVEL_SECONDS[i]) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_SECONDS.length;
    }
}

package com.wimone.enjoytix.order.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.order.config.OrderTimeoutMessageProperties;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("!test")
@RocketMQMessageListener(
        topic = "${order.timeout-message.topic:enjoytix_order_timeout}",
        consumerGroup = "${order.timeout-message.consumer-group:enjoytix_order_timeout_close_cg}"
)
public class RocketMqOrderTimeoutMessageListener
        implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(RocketMqOrderTimeoutMessageListener.class);

    private final ObjectMapper objectMapper;
    private final OrderTimeoutMessageProcessor processor;
    private final OrderTimeoutMessageProperties properties;

    public RocketMqOrderTimeoutMessageListener(
            ObjectMapper objectMapper,
            OrderTimeoutMessageProcessor processor,
            OrderTimeoutMessageProperties properties) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.properties = properties;
    }

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            OrderTimeoutMessage message = objectMapper.readValue(
                    new String(messageExt.getBody(), StandardCharsets.UTF_8),
                    OrderTimeoutMessage.class
            );
            processor.consume(message, messageExt.getReconsumeTimes(), true);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Consume order timeout message failed, msgId={}", messageExt.getMsgId(), ex);
            throw new IllegalStateException("Consume order timeout message failed", ex);
        }
    }

    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        consumer.setMaxReconsumeTimes(properties.getMaxReconsumeTimes());
    }
}

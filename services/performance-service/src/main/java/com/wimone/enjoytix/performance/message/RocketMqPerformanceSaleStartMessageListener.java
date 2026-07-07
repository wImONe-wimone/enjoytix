package com.wimone.enjoytix.performance.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.performance.config.PerformanceSaleMessageProperties;
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
        topic = "${performance.sale-message.topic:enjoytix_performance_sale_start}",
        consumerGroup = "${performance.sale-message.consumer-group:enjoytix_performance_sale_start_cg}"
)
public class RocketMqPerformanceSaleStartMessageListener
        implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(RocketMqPerformanceSaleStartMessageListener.class);

    private final ObjectMapper objectMapper;
    private final PerformanceSaleStartMessageProcessor processor;
    private final PerformanceSaleMessageProperties properties;

    public RocketMqPerformanceSaleStartMessageListener(
            ObjectMapper objectMapper,
            PerformanceSaleStartMessageProcessor processor,
            PerformanceSaleMessageProperties properties) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.properties = properties;
    }

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            PerformanceSaleStartMessage message = objectMapper.readValue(
                    new String(messageExt.getBody(), StandardCharsets.UTF_8),
                    PerformanceSaleStartMessage.class
            );
            processor.consume(message, messageExt.getReconsumeTimes(), true);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Consume performance sale start message failed, msgId={}", messageExt.getMsgId(), ex);
            throw new IllegalStateException("Consume performance sale start message failed", ex);
        }
    }

    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        consumer.setMaxReconsumeTimes(properties.getMaxReconsumeTimes());
    }
}

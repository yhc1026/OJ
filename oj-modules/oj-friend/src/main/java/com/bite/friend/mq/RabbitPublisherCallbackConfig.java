package com.bite.friend.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class RabbitPublisherCallbackConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublisherCallbackConfig.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublisherCallbackConfig(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String id = correlationData == null ? null : correlationData.getId();
                log.error("RabbitMQ publish confirm failed, correlationId={}, cause={}", id, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "RabbitMQ message returned, exchange={}, routingKey={}, replyCode={}, replyText={}, message={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText(),
                returned.getMessage()
        ));
    }
}

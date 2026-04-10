package com.bite.friend.mq;

import com.bite.common.mq.constants.JudgeMqConstants;
import com.bite.common.mq.message.JudgeRunTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JudgeTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public JudgeTaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(JudgeRunTaskMessage message) {
        rabbitTemplate.convertAndSend(
                JudgeMqConstants.JUDGE_EXCHANGE,
                JudgeMqConstants.JUDGE_ROUTING_KEY,
                message
        );
    }
}

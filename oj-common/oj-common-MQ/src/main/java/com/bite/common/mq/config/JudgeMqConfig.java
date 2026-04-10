package com.bite.common.mq.config;

import com.bite.common.mq.constants.JudgeMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class JudgeMqConfig {

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(JudgeMqConstants.JUDGE_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", JudgeMqConstants.JUDGE_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", JudgeMqConstants.JUDGE_DLX_ROUTING_KEY);
        return new Queue(JudgeMqConstants.JUDGE_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding judgeBinding() {
        return BindingBuilder.bind(judgeQueue())
                .to(judgeExchange())
                .with(JudgeMqConstants.JUDGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange judgeDlxExchange() {
        return new DirectExchange(JudgeMqConstants.JUDGE_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeDlxQueue() {
        return new Queue(JudgeMqConstants.JUDGE_DLX_QUEUE, true);
    }

    @Bean
    public Binding judgeDlxBinding() {
        return BindingBuilder.bind(judgeDlxQueue())
                .to(judgeDlxExchange())
                .with(JudgeMqConstants.JUDGE_DLX_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

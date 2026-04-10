package com.bite.common.mq.constants;

public final class JudgeMqConstants {

    private JudgeMqConstants() {
    }

    public static final String JUDGE_EXCHANGE = "oj.judge.exchange";
    public static final String JUDGE_QUEUE = "oj.judge.queue";
    public static final String JUDGE_ROUTING_KEY = "oj.judge.run";

    public static final String JUDGE_DLX_EXCHANGE = "oj.judge.dlx.exchange";
    public static final String JUDGE_DLX_QUEUE = "oj.judge.dlx.queue";
    public static final String JUDGE_DLX_ROUTING_KEY = "oj.judge.run.dlx";
}

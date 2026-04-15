package com.bite.common.aop.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 判题沙箱 compile / run：耗时与次数（Prometheus：{@code judge_operation_seconds_*} + count）。
 */
@Aspect
public class JudgeSandboxMetricsAspect {

    private final MeterRegistry registry;
    private final Timer compileTimer;
    private final Timer runTimer;

    public JudgeSandboxMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
        this.compileTimer = Timer.builder("judge.operation")
                .description("Judge sandbox compile and run latency")
                .tag("phase", "compile")
                .register(registry);
        this.runTimer = Timer.builder("judge.operation")
                .description("Judge sandbox compile and run latency")
                .tag("phase", "run")
                .register(registry);
    }

    @Around(
            "execution(* com.bite.judge.sandbox.RunAndOutput.compile(..)) "
                    + "|| execution(* com.bite.judge.sandbox.RunAndOutput.run(..))"
    )
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        Timer timer = "compile".equals(pjp.getSignature().getName()) ? compileTimer : runTimer;
        Timer.Sample sample = Timer.start(registry);
        try {
            return pjp.proceed();
        } finally {
            sample.stop(timer);
        }
    }
}

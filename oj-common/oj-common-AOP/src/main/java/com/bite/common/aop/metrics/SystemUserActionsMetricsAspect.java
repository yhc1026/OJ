package com.bite.common.aop.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 管理端：登录、新增用户（注册）次数 → {@code user_actions_total}（module=system,action=login|register）。
 */
@Aspect
public class SystemUserActionsMetricsAspect {

    private final Counter login;
    private final Counter register;

    public SystemUserActionsMetricsAspect(MeterRegistry registry) {
        this.login = Counter.builder("user.actions")
                .description("User login and registration events")
                .tag("module", "system")
                .tag("action", "login")
                .register(registry);
        this.register = Counter.builder("user.actions")
                .description("User login and registration events")
                .tag("module", "system")
                .tag("action", "register")
                .register(registry);
    }

    @Around(
            "execution(* com.bite.system.controller.SysUserController.login(..)) "
                    + "|| execution(* com.bite.system.controller.SysUserController.insertUser(..))"
    )
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        String name = pjp.getSignature().getName();
        if ("login".equals(name)) {
            login.increment();
        } else {
            register.increment();
        }
        return pjp.proceed();
    }
}

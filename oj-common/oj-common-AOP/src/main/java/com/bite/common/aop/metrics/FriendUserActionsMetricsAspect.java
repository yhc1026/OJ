package com.bite.common.aop.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * C 端：注册与各渠道登录次数 → {@code user_actions_total}（module=friend,action=login|register）。
 */
@Aspect
public class FriendUserActionsMetricsAspect {

    private final Counter login;
    private final Counter register;

    public FriendUserActionsMetricsAspect(MeterRegistry registry) {
        this.login = Counter.builder("user.actions")
                .description("User login and registration events")
                .tag("module", "friend")
                .tag("action", "login")
                .register(registry);
        this.register = Counter.builder("user.actions")
                .description("User login and registration events")
                .tag("module", "friend")
                .tag("action", "register")
                .register(registry);
    }

    @Around(
            "execution(* com.bite.friend.controller.UserOperationController.register(..)) "
                    + "|| execution(* com.bite.friend.controller.UserOperationController.loginByPhonePassword(..)) "
                    + "|| execution(* com.bite.friend.controller.UserOperationController.loginByEmailPassword(..)) "
                    + "|| execution(* com.bite.friend.controller.UserOperationController.loginByEmailCode(..))"
    )
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        // 根据方法名决定用哪个计数器
        if ("register".equals(pjp.getSignature().getName())) {
            // register计数器+1
            register.increment();
        } else {
            login.increment();
            // login计数器+1
        }
        return pjp.proceed();
    }
}

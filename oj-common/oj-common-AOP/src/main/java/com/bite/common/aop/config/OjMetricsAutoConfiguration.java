package com.bite.common.aop.config;

import com.bite.common.aop.metrics.FriendUserActionsMetricsAspect;
import com.bite.common.aop.metrics.JudgeSandboxMetricsAspect;
import com.bite.common.aop.metrics.SystemUserActionsMetricsAspect;
import com.bite.common.aop.metrics.XxlJobMetricsAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Micrometer 埋点 AOP；依赖方需已引入 actuator（提供 {@link MeterRegistry}）。
 * <p>
 * 必须晚于 {@link MetricsAutoConfiguration}，否则 {@code MeterRegistry} 尚未注册时
 * {@code @ConditionalOnBean(MeterRegistry)} 会为 false，导致切面从未注册（表现为 Prometheus 无自定义指标）。
 */

/**
 * 自动装配类，避免在每一个切面中反复写入@Component/@Bean这些玩意，把公共的 Bean 统一注册。
 * */
@AutoConfiguration
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@EnableAspectJAutoProxy
public class OjMetricsAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.bite.system.controller.SysUserController")
    public SystemUserActionsMetricsAspect systemUserActionsMetricsAspect(MeterRegistry registry) {
        return new SystemUserActionsMetricsAspect(registry);
    }

    @Bean
    @ConditionalOnClass(name = "com.bite.friend.controller.UserOperationController")
    public FriendUserActionsMetricsAspect friendUserActionsMetricsAspect(MeterRegistry registry) {
        return new FriendUserActionsMetricsAspect(registry);
    }

    @Bean
    @ConditionalOnClass(name = "com.bite.judge.sandbox.RunAndOutput")
    public JudgeSandboxMetricsAspect judgeSandboxMetricsAspect(MeterRegistry registry) {
        return new JudgeSandboxMetricsAspect(registry);
    }

    /**
     * 仅当 classpath 存在 XXL-JOB 注解时注册，避免其他服务加载该切面。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    static class XxlJobMetricsConfiguration {

        @Bean
        public XxlJobMetricsAspect xxlJobMetricsAspect(MeterRegistry registry) {
            return new XxlJobMetricsAspect(registry);
        }
    }
}

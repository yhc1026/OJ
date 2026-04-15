package com.bite.common.aop.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * XXL-JOB 任务每执行一次 +1 → {@code xxl_job_executions_total}（handler=任务名）。
 * 不直接引用 XxlJob 类型，避免无 xxl-job 依赖的模块类加载失败。
 */
@Aspect
public class XxlJobMetricsAspect {

    private static final String XXL_JOB_ANNOTATION = "com.xxl.job.core.handler.annotation.XxlJob";

    private final MeterRegistry registry;

    public XxlJobMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        String handler = resolveHandlerName(pjp);
        if (handler != null) {
            registry.counter("xxl.job.executions", "handler", handler).increment();
        }
        return pjp.proceed();
    }

    private static String resolveHandlerName(ProceedingJoinPoint pjp) {
        try {
            MethodSignature ms = (MethodSignature) pjp.getSignature();
            Method method = ms.getMethod();
            Class<?> annClass = Class.forName(XXL_JOB_ANNOTATION);
            Object ann = method.getAnnotation(annClass.asSubclass(java.lang.annotation.Annotation.class));
            if (ann == null) {
                return null;
            }
            return (String) annClass.getMethod("value").invoke(ann);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}

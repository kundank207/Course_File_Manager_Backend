package com.mitmeerut.CFM_Portal.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    @Around("execution(* com.mitmeerut.CFM_Portal.Controller.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        if (executionTime > 500) {
            logger.warn("⚠️ SLOW API DETECTED: {}.{} executed in {}ms", className, methodName, executionTime);
        } else {
            logger.info("✅ API PERFORMANCE: {}.{} executed in {}ms", className, methodName, executionTime);
        }

        return proceed;
    }
}

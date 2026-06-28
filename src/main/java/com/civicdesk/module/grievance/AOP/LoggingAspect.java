package com.civicdesk.module.grievance.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // ✅ Apply ONLY to grievance service layer
    @Around("execution(* com.civicdesk.module.grievance..*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.info("START -> {}.{}()", className, methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("END -> {}.{}() | Time: {} ms",
                    className, methodName, timeTaken);

            return result;

        } catch (Exception ex) {

            log.error("ERROR -> {}.{}() | Exception: {}",
                    className, methodName, ex.getMessage());

            throw ex;
        }
    }
}
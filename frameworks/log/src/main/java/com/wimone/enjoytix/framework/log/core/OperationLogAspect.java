package com.wimone.enjoytix.framework.log.core;

import com.alibaba.fastjson2.JSON;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String operationName = operationLog.value().isBlank()
                ? joinPoint.getSignature().toShortString()
                : operationLog.value();
        try {
            if (operationLog.logArgs()) {
                log.info("Operation start: name={}, args={}", operationName, JSON.toJSONString(joinPoint.getArgs()));
            } else {
                log.info("Operation start: name={}", operationName);
            }
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            if (operationLog.logResult()) {
                log.info("Operation success: name={}, costMs={}, result={}", operationName, cost, JSON.toJSONString(result));
            } else {
                log.info("Operation success: name={}, costMs={}", operationName, cost);
            }
            return result;
        } catch (Throwable ex) {
            log.warn("Operation failed: name={}, costMs={}", operationName, System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }
}

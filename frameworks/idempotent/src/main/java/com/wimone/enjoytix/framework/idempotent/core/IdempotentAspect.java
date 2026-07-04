package com.wimone.enjoytix.framework.idempotent.core;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.idempotent.annotation.Idempotent;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Aspect
public class IdempotentAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public IdempotentAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint, idempotent);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", idempotent.keyTimeout(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ClientException(idempotent.message());
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            stringRedisTemplate.delete(key);
            throw ex;
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String key;
        if (idempotent.type() == IdempotentTypeEnum.SPEL && !idempotent.key().isBlank()) {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(),
                    method,
                    joinPoint.getArgs(),
                    parameterNameDiscoverer
            );
            key = expressionParser.parseExpression(idempotent.key()).getValue(context, String.class);
        } else {
            key = method.toGenericString() + ":" + Arrays.deepHashCode(joinPoint.getArgs());
        }
        return idempotent.uniqueKeyPrefix() + key;
    }
}

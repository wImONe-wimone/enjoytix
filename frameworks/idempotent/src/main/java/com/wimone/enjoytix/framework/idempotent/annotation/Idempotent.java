package com.wimone.enjoytix.framework.idempotent.annotation;

import com.wimone.enjoytix.framework.idempotent.enums.IdempotentSceneEnum;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    String key() default "";

    String uniqueKeyPrefix() default "enjoytix:idempotent:";

    String message() default "Duplicate request";

    long keyTimeout() default 3600L;

    IdempotentTypeEnum type() default IdempotentTypeEnum.PARAM;

    IdempotentSceneEnum scene() default IdempotentSceneEnum.RESTAPI;
}

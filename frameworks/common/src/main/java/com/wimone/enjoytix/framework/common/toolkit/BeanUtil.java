package com.wimone.enjoytix.framework.common.toolkit;

import java.lang.reflect.Constructor;

public final class BeanUtil {

    private BeanUtil() {
    }

    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T target = constructor.newInstance();
            cn.hutool.core.bean.BeanUtil.copyProperties(source, target);
            return target;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Create target bean failed: " + targetClass.getName(), ex);
        }
    }
}

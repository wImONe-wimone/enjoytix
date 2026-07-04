package com.wimone.enjoytix.framework.cache;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
    }

    public static String build(String prefix, Object... parts) {
        String suffix = Arrays.stream(parts)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return prefix + ":" + suffix;
    }
}

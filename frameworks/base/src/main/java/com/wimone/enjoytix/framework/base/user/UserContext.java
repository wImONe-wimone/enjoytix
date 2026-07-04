package com.wimone.enjoytix.framework.base.user;

public final class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfoDTO userInfo) {
        USER_HOLDER.set(userInfo);
    }

    public static UserInfoDTO get() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        UserInfoDTO userInfo = get();
        return userInfo == null ? null : userInfo.getUserId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}

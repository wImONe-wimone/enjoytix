package com.wimone.enjoytix.framework.base.user;

import java.io.Serializable;

public class UserInfoDTO implements Serializable {

    private Long userId;
    private String username;
    private String realName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}

package com.wimone.enjoytix.user.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.time.LocalDateTime;

@TableName("et_user_session")
public class UserSessionDO extends BaseDO {

    private Long userId;
    private String accessToken;
    private Integer validFlag;
    private LocalDateTime expireTime;
    private LocalDateTime logoutTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getValidFlag() {
        return validFlag;
    }

    public void setValidFlag(Integer validFlag) {
        this.validFlag = validFlag;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }
}

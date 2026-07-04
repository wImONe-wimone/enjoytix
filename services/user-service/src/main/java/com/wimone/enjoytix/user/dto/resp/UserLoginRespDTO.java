package com.wimone.enjoytix.user.dto.resp;

public class UserLoginRespDTO {

    private String accessToken;
    private UserRespDTO user;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UserRespDTO getUser() {
        return user;
    }

    public void setUser(UserRespDTO user) {
        this.user = user;
    }
}

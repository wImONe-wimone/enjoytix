package com.wimone.enjoytix.user.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User login response.")
public class UserLoginRespDTO {

    @Schema(description = "Access token for gateway authorization.", example = "dev-1")
    private String accessToken;
    @Schema(description = "Logged-in user profile.")
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

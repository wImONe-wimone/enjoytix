package com.wimone.enjoytix.user.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile response.")
public class UserRespDTO {

    @Schema(description = "User id.", example = "1")
    private Long userId;
    @Schema(description = "Login username.", example = "alice")
    private String username;
    @Schema(description = "Mobile number.", example = "13800000000")
    private String mobile;
    @Schema(description = "Real name.", example = "Alice")
    private String realName;
    @Schema(description = "User status.", example = "1")
    private Integer status;

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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

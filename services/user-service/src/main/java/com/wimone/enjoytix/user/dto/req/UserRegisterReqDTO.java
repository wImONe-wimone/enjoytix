package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User registration request.")
public class UserRegisterReqDTO {

    @NotBlank
    @Schema(description = "Login username.", example = "alice")
    private String username;

    @NotBlank
    @Schema(description = "Login password.", example = "Passw0rd!")
    private String password;

    @NotBlank
    @Schema(description = "Mobile number.", example = "13800000000")
    private String mobile;

    @Schema(description = "Real name for optional identity verification.", example = "Alice")
    private String realName;

    @Schema(description = "Identity card number.", example = "110101199001011234")
    private String idCard;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}

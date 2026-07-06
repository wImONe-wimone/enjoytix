package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User profile update request.")
public class UserProfileUpdateReqDTO {

    @NotBlank
    @Schema(description = "Mobile number.", example = "13800000000")
    private String mobile;

    @Schema(description = "Real name.", example = "Alice")
    private String realName;

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

}

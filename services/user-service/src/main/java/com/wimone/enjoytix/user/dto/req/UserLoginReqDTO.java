package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request.")
public class UserLoginReqDTO {

    @NotBlank
    @Schema(description = "Login username.", example = "alice")
    private String username;

    @NotBlank
    @Schema(description = "Login password.", example = "Passw0rd!")
    private String password;

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
}

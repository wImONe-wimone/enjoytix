package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.UserLoginReqDTO;
import com.wimone.enjoytix.user.dto.req.UserProfileUpdateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;
import com.wimone.enjoytix.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user")
@Tag(name = "User API", description = "User registration, login, profile, and logout APIs.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @OperationLog("user-register")
    @Operation(summary = "Register user", description = "Create a user account and return the user profile.")
    @PostMapping("/register")
    public Result<UserRespDTO> register(@Valid @RequestBody UserRegisterReqDTO requestParam) {
        return Results.success(userService.register(requestParam));
    }

    @OperationLog("user-login")
    @Operation(summary = "Login user", description = "Validate credentials and return an access token.")
    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@Valid @RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    @Operation(summary = "Get current user", description = "Query the current user profile by gateway propagated user id.")
    @GetMapping("/me")
    public Result<UserRespDTO> me(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(userService.queryByUserId(userId));
    }

    @OperationLog("user-profile-update")
    @Operation(summary = "Update current user profile", description = "Update mobile and real name for the current user.")
    @PutMapping("/me")
    public Result<UserRespDTO> updateMe(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UserProfileUpdateReqDTO requestParam) {
        return Results.success(userService.updateProfile(userId, requestParam));
    }

    @Operation(summary = "Get user by id", description = "Query a user profile by user id.")
    @GetMapping("/{userId}")
    public Result<UserRespDTO> queryByUserId(
            @Parameter(description = "User id.", required = true)
            @PathVariable Long userId) {
        return Results.success(userService.queryByUserId(userId));
    }

    @OperationLog("user-logout")
    @Operation(summary = "Logout user", description = "Invalidate current user login sessions recorded by user-service.")
    @PostMapping("/logout")
    public Result<Boolean> logout(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Authorization header.", required = false)
            @RequestHeader(value = UserConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        return Results.success(userService.logout(userId, authorization));
    }
}

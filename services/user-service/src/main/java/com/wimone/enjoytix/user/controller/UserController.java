package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.UserLoginReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;
import com.wimone.enjoytix.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @OperationLog("user-register")
    @PostMapping("/register")
    public Result<UserRespDTO> register(@Valid @RequestBody UserRegisterReqDTO requestParam) {
        return Results.success(userService.register(requestParam));
    }

    @OperationLog("user-login")
    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@Valid @RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    @GetMapping("/me")
    public Result<UserRespDTO> me(@RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(userService.queryByUserId(userId));
    }

    @GetMapping("/{userId}")
    public Result<UserRespDTO> queryByUserId(@PathVariable Long userId) {
        return Results.success(userService.queryByUserId(userId));
    }

    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Results.success(Boolean.TRUE);
    }
}

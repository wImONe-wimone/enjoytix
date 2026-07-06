package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.UserAddressCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserAddressUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserAddressRespDTO;
import com.wimone.enjoytix.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user/addresses")
@Tag(name = "User Address API", description = "Current user address management APIs.")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @Operation(summary = "List addresses", description = "List all addresses owned by the current user.")
    @GetMapping
    public Result<List<UserAddressRespDTO>> list(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(userAddressService.list(userId));
    }

    @Operation(summary = "Get address", description = "Get an address owned by the current user.")
    @GetMapping("/{addressId}")
    public Result<UserAddressRespDTO> detail(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Address id.", required = true)
            @PathVariable Long addressId) {
        return Results.success(userAddressService.detail(userId, addressId));
    }

    @OperationLog("user-address-create")
    @Operation(summary = "Create address", description = "Create a delivery/contact address for the current user.")
    @PostMapping
    public Result<UserAddressRespDTO> create(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UserAddressCreateReqDTO requestParam) {
        return Results.success(userAddressService.create(userId, requestParam));
    }

    @OperationLog("user-address-update")
    @Operation(summary = "Update address", description = "Update an address owned by the current user.")
    @PutMapping
    public Result<UserAddressRespDTO> update(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UserAddressUpdateReqDTO requestParam) {
        return Results.success(userAddressService.update(userId, requestParam));
    }

    @OperationLog("user-address-set-default")
    @Operation(summary = "Set default address", description = "Set an address as the default address for the current user.")
    @PutMapping("/{addressId}/default")
    public Result<UserAddressRespDTO> setDefault(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Address id.", required = true)
            @PathVariable Long addressId) {
        return Results.success(userAddressService.setDefault(userId, addressId));
    }

    @OperationLog("user-address-delete")
    @Operation(summary = "Delete address", description = "Delete an address owned by the current user.")
    @DeleteMapping("/{addressId}")
    public Result<Boolean> delete(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Address id.", required = true)
            @PathVariable Long addressId) {
        return Results.success(userAddressService.delete(userId, addressId));
    }
}

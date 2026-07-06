package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.UserOrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.user.service.UserOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user/orders")
@Tag(name = "My Order API", description = "Current user order query and management APIs.")
public class UserOrderController {

    private final UserOrderService userOrderService;

    public UserOrderController(UserOrderService userOrderService) {
        this.userOrderService = userOrderService;
    }

    @Operation(summary = "List my orders", description = "List orders owned by the current user via order-service.")
    @GetMapping
    public Result<List<OrderDetailRespDTO>> list(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(userOrderService.list(userId));
    }

    @Operation(summary = "Get my order detail", description = "Get an order detail owned by the current user via order-service.")
    @GetMapping("/{orderId}")
    public Result<OrderDetailRespDTO> detail(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Order id.", required = true)
            @PathVariable Long orderId) {
        return Results.success(userOrderService.detail(userId, orderId));
    }

    @OperationLog("user-order-cancel")
    @Operation(summary = "Cancel my order", description = "Cancel a pending payment order owned by the current user via order-service.")
    @PostMapping("/cancel")
    public Result<Boolean> cancel(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UserOrderCancelReqDTO requestParam) {
        return Results.success(userOrderService.cancel(userId, requestParam));
    }
}

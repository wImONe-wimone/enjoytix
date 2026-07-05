package com.wimone.enjoytix.order.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.idempotent.annotation.Idempotent;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.order.common.OrderConstants;
import com.wimone.enjoytix.order.dto.req.OrderCancelReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderCreateReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.order.dto.resp.OrderCreateRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderDetailRespDTO;
import com.wimone.enjoytix.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/order")
@Tag(name = "Order API", description = "Order creation, cancellation, payment confirmation, timeout close, and query APIs.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建待支付订单
     */
    @OperationLog("order-create")
    @Operation(summary = "Create order", description = "Create a pending payment order after locking ticket inventory or seats.")
    @Idempotent(
            key = "'order:create:' + #p0 + ':' + #p1.showId + ':' + #p1.categoryId + ':' + #p1.quantity + ':' + #p1.seatIds",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Order is being created, please do not submit repeatedly"
    )
    @PostMapping("/create")
    public Result<OrderCreateRespDTO> create(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order creation request. showId, categoryId, and seatIds must belong to the same show session.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderCreateReqDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Concert VIP seat",
                                            value = "{\"showId\":2001,\"categoryId\":3001,\"quantity\":1,\"seatIds\":[400101]}"
                                    ),
                                    @ExampleObject(
                                            name = "Drama standard seat",
                                            value = "{\"showId\":2002,\"categoryId\":3004,\"quantity\":1,\"seatIds\":[401101]}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody OrderCreateReqDTO requestParam) {
        return Results.success(orderService.create(userId, requestParam));
    }

    /**
     * 取消待支付订单
     */
    @OperationLog("order-cancel")
    @Operation(summary = "Cancel order", description = "Cancel a pending payment order and release its ticket lock.")
    @Idempotent(
            key = "'order:cancel:' + #p0 + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Order cancel request is being processed"
    )
    @PostMapping("/cancel")
    public Result<Boolean> cancel(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody OrderCancelReqDTO requestParam) {
        return Results.success(orderService.cancel(userId, requestParam));
    }

    /**
     * 支付成功通知入口
     */
    @OperationLog("order-pay-success")
    @Operation(summary = "Confirm payment success", description = "Internal payment success notification that issues tickets for the order.")
    @Idempotent(
            key = "'order:pay-success:' + #p0.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Payment callback is being processed"
    )
    @PostMapping("/pay-success")
    public Result<OrderDetailRespDTO> paySuccess(@Valid @RequestBody OrderPaySuccessReqDTO requestParam) {
        return Results.success(orderService.paySuccess(requestParam));
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "Get order detail", description = "Query order detail for the current user.")
    @GetMapping("/{orderId}")
    public Result<OrderDetailRespDTO> detail(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Order id.", required = true)
            @PathVariable Long orderId) {
        return Results.success(orderService.detail(userId, orderId));
    }

    /**
     * 查询当前用户订单列表
     */
    @Operation(summary = "List user orders", description = "List orders owned by the current user.")
    @GetMapping("/page")
    public Result<List<OrderDetailRespDTO>> list(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId) {
        return Results.success(orderService.list(userId));
    }
}

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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @OperationLog("order-create")
    @Idempotent(
            key = "'order:create:' + #p0 + ':' + #p1.showId + ':' + #p1.categoryId + ':' + #p1.quantity + ':' + #p1.seatIds",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Order is being created, please do not submit repeatedly"
    )
    @PostMapping("/create")
    public Result<OrderCreateRespDTO> create(
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody OrderCreateReqDTO requestParam) {
        return Results.success(orderService.create(userId, requestParam));
    }

    @OperationLog("order-cancel")
    @Idempotent(
            key = "'order:cancel:' + #p0 + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Order cancel request is being processed"
    )
    @PostMapping("/cancel")
    public Result<Boolean> cancel(
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody OrderCancelReqDTO requestParam) {
        return Results.success(orderService.cancel(userId, requestParam));
    }

    @OperationLog("order-pay-success")
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

    @GetMapping("/{orderId}")
    public Result<OrderDetailRespDTO> detail(
            @RequestHeader(OrderConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long orderId) {
        return Results.success(orderService.detail(userId, orderId));
    }

    @GetMapping("/page")
    public Result<List<OrderDetailRespDTO>> list(@RequestHeader(OrderConstants.USER_ID_HEADER) Long userId) {
        return Results.success(orderService.list(userId));
    }
}

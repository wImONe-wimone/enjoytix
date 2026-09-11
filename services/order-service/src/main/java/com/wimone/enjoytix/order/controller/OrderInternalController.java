package com.wimone.enjoytix.order.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.order.dto.resp.OrderPurchaseCheckRespDTO;
import com.wimone.enjoytix.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/order/internal")
@Tag(name = "Order Internal API", description = "Internal order APIs for service collaboration.")
public class OrderInternalController {

    private final OrderService orderService;

    public OrderInternalController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Check performance purchase", description = "Check whether a user has a paid order for a performance.")
    @GetMapping("/purchases/check")
    public Result<OrderPurchaseCheckRespDTO> checkPurchase(
            @Parameter(description = "User id.", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Performance id.", required = true)
            @RequestParam Long performanceId) {
        return Results.success(orderService.checkPurchase(userId, performanceId));
    }
}

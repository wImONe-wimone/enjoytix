package com.wimone.enjoytix.pay.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.idempotent.annotation.Idempotent;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.pay.common.PayConstants;
import com.wimone.enjoytix.pay.dto.req.MockPayReqDTO;
import com.wimone.enjoytix.pay.dto.req.PayCreateReqDTO;
import com.wimone.enjoytix.pay.dto.resp.PayRespDTO;
import com.wimone.enjoytix.pay.service.PayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @OperationLog("pay-create")
    @Idempotent(
            key = "'pay:create:' + #p0 + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Pay order is being created, please do not submit repeatedly"
    )
    @PostMapping("/create")
    public Result<PayRespDTO> create(
            @RequestHeader(PayConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody PayCreateReqDTO requestParam) {
        return Results.success(payService.create(userId, requestParam));
    }

    @OperationLog("pay-mock-success")
    @Idempotent(
            key = "'pay:mock-success:' + #p0 + ':' + #p1.payId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Pay success request is being processed"
    )
    @PostMapping("/mock-success")
    public Result<PayRespDTO> mockSuccess(
            @RequestHeader(PayConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody MockPayReqDTO requestParam) {
        return Results.success(payService.mockSuccess(userId, requestParam));
    }

    @GetMapping("/{payId}")
    public Result<PayRespDTO> detail(
            @RequestHeader(PayConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long payId) {
        return Results.success(payService.detail(userId, payId));
    }
}

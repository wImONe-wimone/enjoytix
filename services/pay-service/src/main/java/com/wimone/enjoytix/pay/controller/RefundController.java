package com.wimone.enjoytix.pay.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.idempotent.annotation.Idempotent;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.pay.common.PayConstants;
import com.wimone.enjoytix.pay.dto.req.RefundApplyReqDTO;
import com.wimone.enjoytix.pay.dto.resp.RefundRespDTO;
import com.wimone.enjoytix.pay.service.PayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refund")
public class RefundController {

    private final PayService payService;

    public RefundController(PayService payService) {
        this.payService = payService;
    }

    @OperationLog("refund-apply")
    @Idempotent(
            key = "'refund:apply:' + #p0 + ':' + #p1.payId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 60,
            message = "Refund request is being processed"
    )
    @PostMapping("/apply")
    public Result<RefundRespDTO> apply(
            @RequestHeader(PayConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody RefundApplyReqDTO requestParam) {
        return Results.success(payService.refund(userId, requestParam));
    }
}

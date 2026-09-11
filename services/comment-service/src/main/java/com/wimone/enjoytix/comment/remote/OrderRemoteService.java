package com.wimone.enjoytix.comment.remote;

import com.wimone.enjoytix.comment.remote.dto.OrderPurchaseCheckRespDTO;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "enjoytix-order-service")
public interface OrderRemoteService {

    @GetMapping("/api/order/internal/purchases/check")
    Result<OrderPurchaseCheckRespDTO> checkPurchase(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("performanceId") Long performanceId);
}

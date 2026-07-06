package com.wimone.enjoytix.user.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.user.remote.dto.RefundByOrderReqDTO;
import com.wimone.enjoytix.user.remote.dto.RefundRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "enjoytix-pay-service")
public interface PayRemoteService {

    @PostMapping("/api/refund/apply-by-order")
    Result<RefundRespDTO> applyByOrder(@RequestHeader("X-User-Id") Long userId, @RequestBody RefundByOrderReqDTO requestParam);
}

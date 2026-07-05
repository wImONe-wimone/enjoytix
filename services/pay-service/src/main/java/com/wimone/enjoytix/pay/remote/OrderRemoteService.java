package com.wimone.enjoytix.pay.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.pay.remote.dto.OrderPaySuccessReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "enjoytix-order-service")
public interface OrderRemoteService {

    @GetMapping("/api/order/{orderId}")
    Result<OrderDetailRespDTO> detail(@RequestHeader("X-User-Id") Long userId, @PathVariable("orderId") Long orderId);

    @PostMapping("/api/order/pay-success")
    Result<OrderDetailRespDTO> paySuccess(@RequestBody OrderPaySuccessReqDTO requestParam);
}

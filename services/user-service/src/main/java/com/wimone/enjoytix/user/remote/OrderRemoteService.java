package com.wimone.enjoytix.user.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.user.remote.dto.OrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundApplyReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundCompleteReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundRollbackReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "enjoytix-order-service")
public interface OrderRemoteService {

    @GetMapping("/api/order/page")
    Result<List<OrderDetailRespDTO>> list(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/order/{orderId}")
    Result<OrderDetailRespDTO> detail(@RequestHeader("X-User-Id") Long userId, @PathVariable("orderId") Long orderId);

    @PostMapping("/api/order/cancel")
    Result<Boolean> cancel(@RequestHeader("X-User-Id") Long userId, @RequestBody OrderCancelReqDTO requestParam);

    @PostMapping("/api/order/refund/apply")
    Result<OrderDetailRespDTO> applyRefund(@RequestHeader("X-User-Id") Long userId, @RequestBody OrderRefundApplyReqDTO requestParam);

    @PostMapping("/api/order/refund/complete")
    Result<OrderDetailRespDTO> completeRefund(@RequestHeader("X-User-Id") Long userId, @RequestBody OrderRefundCompleteReqDTO requestParam);

    @PostMapping("/api/order/refund/rollback")
    Result<OrderDetailRespDTO> rollbackRefund(@RequestHeader("X-User-Id") Long userId, @RequestBody OrderRefundRollbackReqDTO requestParam);
}

package com.wimone.enjoytix.agent.remote;
import com.wimone.enjoytix.agent.remote.dto.OrderDetailResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
@FeignClient(name = "enjoytix-order-service", contextId = "orderReadRemoteService")
public interface OrderReadRemoteService {
    @GetMapping("/api/order/{orderId}") Result<OrderDetailResponse> detail(@PathVariable("orderId") Long orderId);
    @GetMapping("/api/order/page") Result<List<OrderDetailResponse>> page();
}

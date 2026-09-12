package com.wimone.enjoytix.agent.remote;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.agent.trade.AgentOrderCreateRequest;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@FeignClient(name = "enjoytix-order-service", contextId = "agentOrderCreateRemoteService")
public interface AgentOrderCreateRemoteService {
    @PostMapping("/api/order/internal/agent/create")
    Result<AgentOrderCreateResponse> create(@RequestBody AgentOrderCreateRequest request);
}

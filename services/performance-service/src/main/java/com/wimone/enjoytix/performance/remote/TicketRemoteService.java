package com.wimone.enjoytix.performance.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockConfigInitReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockInitReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "enjoytix-ticket-service")
public interface TicketRemoteService {

    @PostMapping("/api/ticket/admin/shows/init-stock")
    Result<Boolean> initShowStock(@RequestHeader("X-User-Id") Long operatorId, @RequestBody TicketShowStockInitReqDTO requestParam);

    @PostMapping("/api/ticket/admin/shows/init-stock/config")
    Result<Boolean> initConfiguredShowStock(@RequestHeader("X-User-Id") Long operatorId, @RequestBody TicketShowStockConfigInitReqDTO requestParam);
}

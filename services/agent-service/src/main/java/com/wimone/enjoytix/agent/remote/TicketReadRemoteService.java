package com.wimone.enjoytix.agent.remote;
import com.wimone.enjoytix.agent.remote.dto.*;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
@FeignClient(name = "enjoytix-ticket-service")
public interface TicketReadRemoteService {
    @GetMapping("/api/ticket/availability") Result<List<TicketAvailabilityResponse>> availability(@RequestParam("showId") Long showId);
    @GetMapping("/api/ticket/seats") Result<List<SeatAvailabilityResponse>> seats(@RequestParam("showId") Long showId);
}

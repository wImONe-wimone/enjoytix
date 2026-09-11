package com.wimone.enjoytix.order.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.order.remote.dto.ShowSessionRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "enjoytix-performance-service")
public interface PerformanceRemoteService {

    @GetMapping("/api/show/{showId}")
    Result<ShowSessionRespDTO> show(@PathVariable("showId") Long showId);
}

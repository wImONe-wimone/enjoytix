package com.wimone.enjoytix.comment.remote;

import com.wimone.enjoytix.comment.remote.dto.PerformanceDetailRespDTO;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "enjoytix-performance-service")
public interface PerformanceRemoteService {

    @GetMapping("/api/performance/{performanceId}")
    Result<PerformanceDetailRespDTO> detail(@PathVariable("performanceId") Long performanceId);
}

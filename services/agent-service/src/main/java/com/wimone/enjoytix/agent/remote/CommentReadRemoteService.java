package com.wimone.enjoytix.agent.remote;

import com.wimone.enjoytix.agent.remote.dto.PerformanceRatingSummaryResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "enjoytix-comment-service")
public interface CommentReadRemoteService {
    @GetMapping("/api/comment/performances/{performanceId}/rating-summary")
    Result<PerformanceRatingSummaryResponse> ratingSummary(@PathVariable("performanceId") Long performanceId);
}

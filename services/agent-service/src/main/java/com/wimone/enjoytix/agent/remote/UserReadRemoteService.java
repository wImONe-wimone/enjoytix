package com.wimone.enjoytix.agent.remote;
import com.wimone.enjoytix.agent.remote.dto.CurrentUserResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
@FeignClient(name = "enjoytix-user-service")
public interface UserReadRemoteService {
    @GetMapping("/api/user/me") Result<CurrentUserResponse> me();
}

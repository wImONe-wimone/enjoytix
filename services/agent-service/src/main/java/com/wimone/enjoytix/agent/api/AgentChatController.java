package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.service.AgentChatResult;
import com.wimone.enjoytix.agent.service.AgentOrchestrator;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentChatController {
    private final AgentOrchestrator orchestrator;

    public AgentChatController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public Result<AgentChatResult> chat(@Valid @RequestBody AgentMessageReq request) {
        return Results.success(orchestrator.chat(request.content()));
    }
}
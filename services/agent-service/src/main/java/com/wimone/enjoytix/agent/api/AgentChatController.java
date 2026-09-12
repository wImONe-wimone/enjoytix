package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.service.AgentChatResult;
import com.wimone.enjoytix.agent.service.AgentOrchestrator;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentChatController {
    private final AgentOrchestrator orchestrator;
    private final AgentAuthenticationContext authenticationContext;

    public AgentChatController(AgentOrchestrator orchestrator) {
        this(orchestrator, null);
    }

    @Autowired
    public AgentChatController(AgentOrchestrator orchestrator, AgentAuthenticationContext authenticationContext) {
        this.orchestrator = orchestrator;
        this.authenticationContext = authenticationContext;
    }

    @PostMapping("/chat")
    public Result<AgentChatResult> chat(HttpServletRequest request, @Valid @RequestBody AgentMessageReq messageRequest) {
        AgentUserContext user = resolve(request);
        AgentUserContext previous = AgentUserContextHolder.current();
        try {
            AgentUserContextHolder.set(user);
            return Results.success(orchestrator.chat(messageRequest.content()));
        } finally {
            if (previous == null) AgentUserContextHolder.clear(); else AgentUserContextHolder.set(previous);
        }
    }

    private AgentUserContext resolve(HttpServletRequest request) {
        if (authenticationContext != null) return authenticationContext.resolve(request);
        String value = request.getHeader("X-User-Id");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Authenticated user context is required");
        return new AgentUserContext(Long.valueOf(value), request.getHeader("X-Username"));
    }
}
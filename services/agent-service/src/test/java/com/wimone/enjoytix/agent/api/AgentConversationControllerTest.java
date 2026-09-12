package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentAuthenticationPort;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.model.AgentConversation;
import java.time.Instant;
import java.util.List;
import com.wimone.enjoytix.agent.service.AgentApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConversationControllerTest {
    @Test
    void ignoresClientSuppliedUserIdWhenLoadingConversation() {
        AgentApplicationService applicationService = mock(AgentApplicationService.class);
        when(applicationService.getConversation(88L, 1001L)).thenReturn(new AgentConversation(88L, 1001L, Instant.now(), List.of()));
        AgentAuthenticationPort port = request -> new AgentUserContext(1001L, "alice");
        AgentConversationController controller = new AgentConversationController(applicationService, new AgentAuthenticationContext(port));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "2002");
        controller.get(request, 88L);
        verify(applicationService).getConversation(eq(88L), eq(1001L));
    }
}

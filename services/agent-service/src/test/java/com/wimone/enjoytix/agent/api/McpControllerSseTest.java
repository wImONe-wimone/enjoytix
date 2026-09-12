package com.wimone.enjoytix.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.InMemoryAgentToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpControllerSseTest {
    @Test
    void opensMcpSseStream() throws Exception {
        McpController controller = new McpController(new InMemoryAgentToolRegistry(), new ObjectMapper());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/mcp/sse"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(request().asyncStarted());
    }
}

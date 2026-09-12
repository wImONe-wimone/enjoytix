package com.wimone.enjoytix.agent.poc;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.framework.web.config.OpenApiAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentCompatibilityPocApplicationTest {
    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Autowired(required = false)
    private WebMvcSseServerTransportProvider mcpTransport;

    @Test
    void startsAgentContextWithSpringAiMcpAndSaToken() {
        assertThat(context).isNotNull();
        assertThat(chatClientBuilder).isNotNull();
        assertThat(mcpTransport).isNotNull();
        assertThat(SaManager.getSaTokenContext()).isInstanceOf(SaTokenContext.class);
        assertThat(context.getBean(IdGeneratorManager.class)).isNotNull();
        assertThat(context.getBean(OpenApiAutoConfiguration.class)).isNotNull();
    }
}

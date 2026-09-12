package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleAgentModelClientTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsToolsAndParsesToolCallResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(request).contains("search_show_sessions");
            byte[] response = "{\"choices\":[{\"message\":{\"content\":null,\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"search_show_sessions\",\"arguments\":\"{\\\"city\\\":\\\"Shanghai\\\"}\"}}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenAiCompatibleAgentModelClient client = new OpenAiCompatibleAgentModelClient(new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "key", "model",
                java.time.Duration.ofSeconds(3), java.time.Duration.ofSeconds(5));

        AgentModelResponse response = client.complete(new AgentModelRequest(
                List.of(AgentModelMessage.user("find shows")),
                List.of(new AgentModelToolDefinition("search_show_sessions", "Search", Map.of("type", "object")))));

        assertThat(response.toolCalls()).singleElement().satisfies(call -> {
            assertThat(call.name()).isEqualTo("search_show_sessions");
            assertThat(call.arguments()).containsEntry("city", "Shanghai");
        });
    }
}
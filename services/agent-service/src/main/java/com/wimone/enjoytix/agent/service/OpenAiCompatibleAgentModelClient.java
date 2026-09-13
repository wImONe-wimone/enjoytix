package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${agent.model.enabled:false}' == 'true' and '${agent.model.tool-calling-enabled:false}' == 'true' and '${agent.model.provider:disabled}' == 'openai'")
public class OpenAiCompatibleAgentModelClient implements AgentModelClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration readTimeout;

    public OpenAiCompatibleAgentModelClient(ObjectMapper objectMapper,
                                            @Value("${agent.model.base-url}") String baseUrl,
                                            @Value("${agent.model.api-key:}") String apiKey,
                                            @Value("${agent.model.name:default}") String model,
                                            @Value("${agent.model.connect-timeout:3s}") Duration connectTimeout,
                                            @Value("${agent.model.read-timeout:60s}") Duration readTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.model = model;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override
    public AgentModelResponse complete(AgentModelRequest request) {
        if (apiKey.isBlank()) {
            throw new AgentModelException("Agent model API key is not configured");
        }
        try {
            String body = objectMapper.writeValueAsString(toPayload(request));
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(readTimeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentModelException("Agent model returned HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (IOException ex) {
            throw new AgentModelException("Agent model request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AgentModelException("Agent model request interrupted", ex);
        }
    }

    private Map<String, Object> toPayload(AgentModelRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("messages", request.messages().stream().map(this::toMessage).toList());
        payload.put("tools", request.tools().stream().map(tool -> Map.of(
                "type", "function",
                "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.inputSchema())
        )).toList());
        payload.put("tool_choice", "auto");
        return payload;
    }

    private Map<String, Object> toMessage(AgentModelMessage message) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("role", message.role());
        value.put("content", message.content());
        if ("tool".equals(message.role())) {
            value.put("tool_call_id", message.toolCallId());
            value.put("name", message.name());
        }
        if (!message.toolCalls().isEmpty()) {
            value.put("tool_calls", message.toolCalls().stream().map(call -> Map.of(
                    "id", call.id(), "type", "function",
                    "function", Map.of("name", call.name(), "arguments", writeArguments(call.arguments()))
            )).toList());
        }
        return value;
    }

    private AgentModelResponse parseResponse(String body) {
        try {
            JsonNode message = objectMapper.readTree(body).at("/choices/0/message");
            List<AgentToolCall> calls = new ArrayList<>();
            for (JsonNode call : message.path("tool_calls")) {
                JsonNode function = call.path("function");
                Map<String, Object> arguments = objectMapper.readValue(function.path("arguments").asText("{}"), Map.class);
                calls.add(new AgentToolCall(call.path("id").asText(), function.path("name").asText(), arguments));
            }
            return new AgentModelResponse(message.path("content").isNull() ? null : message.path("content").asText(null), calls);
        } catch (IOException ex) {
            throw new AgentModelException("Invalid agent model response", ex);
        }
    }

    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (IOException ex) {
            throw new AgentModelException("Unable to serialize tool arguments", ex);
        }
    }
}

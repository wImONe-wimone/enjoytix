package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "agent.model.enabled", havingValue = "true")
public class OpenAiCompatibleAgentResponseGenerator implements AgentResponseGenerator, StreamingAgentResponseGenerator {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration readTimeout;

    public OpenAiCompatibleAgentResponseGenerator(ObjectMapper objectMapper,
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
    public String generate(Long conversationId, Long userId, String content) {
        return generateStream(conversationId, userId, content).collect(java.util.stream.Collectors.joining());
    }

    @Override
    public Stream<String> generateStream(Long conversationId, Long userId, String content) {
        if (apiKey.isBlank()) {
            throw new AgentModelException("Agent model API key is not configured");
        }
        try {
            String body = "{\"model\":\"" + escape(model) + "\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"" + escape(content) + "\"}]}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(readTimeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new AgentModelException("Agent model returned HTTP " + response.statusCode());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            return reader.lines().map(this::parseData).filter(Objects::nonNull).onClose(() -> close(reader));
        } catch (IOException ex) {
            throw new AgentModelException("Agent model request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AgentModelException("Agent model request interrupted", ex);
        }
    }

    private String parseData(String line) {
        if (!line.startsWith("data:")) return null;
        String data = line.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) return null;
        try {
            JsonNode content = objectMapper.readTree(data).at("/choices/0/delta/content");
            return content.isTextual() ? content.textValue() : null;
        } catch (IOException ex) {
            throw new AgentModelException("Invalid agent model SSE payload", ex);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void close(BufferedReader reader) {
        try { reader.close(); } catch (IOException ignored) { }
    }
}
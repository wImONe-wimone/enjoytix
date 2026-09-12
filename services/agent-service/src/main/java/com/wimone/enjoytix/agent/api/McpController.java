package com.wimone.enjoytix.agent.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentTool;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private final AgentToolRegistry registry;
    private final AgentToolExecutor executor;
    private final ObjectMapper objectMapper;

    public McpController(AgentToolRegistry registry, ObjectMapper objectMapper) {
        this(registry, toolExecutor(registry), objectMapper);
    }

    @Autowired
    public McpController(AgentToolRegistry registry, AgentToolExecutor executor, ObjectMapper objectMapper) {
        this.registry = registry;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openSse() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        try {
            emitter.send(SseEmitter.event().name("endpoint").data("/mcp", MediaType.TEXT_PLAIN));
        } catch (java.io.IOException ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handle(@RequestBody McpJsonRpcRequest request) {
        if (request == null || request.method() == null || request.method().isBlank()) {
            return error(request == null ? null : request.id(), -32600, "Invalid Request");
        }
        return switch (request.method()) {
            case "initialize" -> success(request.id(), initializeResult());
            case "tools/list" -> success(request.id(), toolsListResult());
            case "tools/call" -> callTool(request);
            case "notifications/initialized" -> success(request.id(), Map.of());
            default -> error(request.id(), -32601, "Method not found: " + request.method());
        };
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "enjoytix-agent-service", "version", "0.1.0"));
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = registry.list().stream().map(this::toolDefinition).toList();
        return Map.of("tools", tools);
    }

    private Map<String, Object> toolDefinition(AgentTool tool) {
        return Map.of("name", tool.name(), "description", tool.description(), "inputSchema", tool.inputSchema());
    }

    private Map<String, Object> callTool(McpJsonRpcRequest request) {
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        Object name = params.get("name");
        if (!(name instanceof String toolName) || toolName.isBlank()) {
            return error(request.id(), -32602, "tools/call requires a non-blank name");
        }
        Object arguments = params.get("arguments");
        Map<String, Object> argumentMap = arguments instanceof Map<?, ?> map ? toStringMap(map) : Map.of();
        AgentToolResult result = executor.execute(toolName, new AgentToolRequest(argumentMap));
        String text = result.success() ? json(result.data()) : result.errorMessage();
        Map<String, Object> content = Map.of("type", "text", "text", text);
        return success(request.id(), Map.of("content", List.of(content), "isError", !result.success()));
    }

    private Map<String, Object> toStringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> { if (key instanceof String name) result.put(name, value); });
        return result;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private Map<String, Object> success(Object id, Object result) {
        Map<String, Object> response = successResult(result);
        response.put("id", id);
        return response;
    }

    private Map<String, Object> successResult(Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    private static AgentToolExecutor toolExecutor(AgentToolRegistry registry) {
        return (toolName, request) -> registry.find(toolName)
                .map(tool -> tool.execute(request))
                .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", "Unknown agent tool: " + toolName));
    }
}

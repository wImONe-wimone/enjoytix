package com.wimone.enjoytix.agent.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.tool.AgentTool;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AgentAuthenticationContext authenticationContext;

    public McpController(AgentToolRegistry registry, ObjectMapper objectMapper) {
        this(registry, toolExecutor(registry), objectMapper, null);
    }

    @Autowired
    public McpController(AgentToolRegistry registry, AgentToolExecutor executor, ObjectMapper objectMapper,
                         AgentAuthenticationContext authenticationContext) {
        this.registry = registry;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.authenticationContext = authenticationContext;
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
    public Map<String, Object> handle(HttpServletRequest request, @RequestBody McpJsonRpcRequest rpcRequest) {
        AgentUserContext previous = AgentUserContextHolder.current();
        if (authenticationContext != null) AgentUserContextHolder.set(authenticationContext.resolve(request));
        try {
            return process(rpcRequest);
        } finally {
            if (previous == null) AgentUserContextHolder.clear(); else AgentUserContextHolder.set(previous);
        }
    }

    public Map<String, Object> handle(McpJsonRpcRequest rpcRequest) {
        return process(rpcRequest);
    }

    private Map<String, Object> process(McpJsonRpcRequest rpcRequest) {
        if (rpcRequest == null || rpcRequest.method() == null || rpcRequest.method().isBlank()) {
            return error(rpcRequest == null ? null : rpcRequest.id(), -32600, "Invalid Request");
        }
        return switch (rpcRequest.method()) {
            case "initialize" -> success(rpcRequest.id(), initializeResult());
            case "tools/list" -> success(rpcRequest.id(), toolsListResult());
            case "tools/call" -> callTool(rpcRequest);
            case "notifications/initialized" -> success(rpcRequest.id(), Map.of());
            default -> error(rpcRequest.id(), -32601, "Method not found: " + rpcRequest.method());
        };
    }

    private Map<String, Object> initializeResult() {
        return Map.of("protocolVersion", PROTOCOL_VERSION, "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "enjoytix-agent-service", "version", "0.1.0"));
    }

    private Map<String, Object> toolsListResult() {
        return Map.of("tools", registry.list().stream().map(this::toolDefinition).toList());
    }

    private Map<String, Object> toolDefinition(AgentTool tool) {
        return Map.of("name", tool.name(), "description", tool.description(), "inputSchema", tool.inputSchema());
    }

    private Map<String, Object> callTool(McpJsonRpcRequest request) {
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        Object name = params.get("name");
        if (!(name instanceof String toolName) || toolName.isBlank()) return error(request.id(), -32602, "tools/call requires a non-blank name");
        Object arguments = params.get("arguments");
        Map<String, Object> argumentMap = arguments instanceof Map<?, ?> map ? toStringMap(map) : Map.of();
        AgentToolResult result = executor.execute(toolName, new AgentToolRequest(argumentMap));
        String text = result.success() ? json(result.data()) : result.errorMessage();
        return success(request.id(), Map.of("content", List.of(Map.of("type", "text", "text", text)), "isError", !result.success()));
    }

    private Map<String, Object> toStringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> { if (key instanceof String name) result.put(name, value); });
        return result;
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException ex) { return "{}"; }
    }

    private Map<String, Object> success(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0"); response.put("id", id); response.put("result", result); return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0"); response.put("id", id); response.put("error", Map.of("code", code, "message", message)); return response;
    }

    private static AgentToolExecutor toolExecutor(AgentToolRegistry registry) {
        return (toolName, request) -> registry.find(toolName).map(tool -> tool.execute(request))
                .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", "Unknown agent tool: " + toolName));
    }
}

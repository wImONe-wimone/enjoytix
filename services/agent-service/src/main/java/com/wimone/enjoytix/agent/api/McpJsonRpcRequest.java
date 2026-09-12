package com.wimone.enjoytix.agent.api;

import java.util.Map;

public record McpJsonRpcRequest(Object id, String method, Map<String, Object> params) {
}

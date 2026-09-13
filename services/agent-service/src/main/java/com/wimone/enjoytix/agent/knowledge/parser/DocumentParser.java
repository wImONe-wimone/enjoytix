package com.wimone.enjoytix.agent.knowledge.parser;

public interface DocumentParser {
    ParsedDocument parse(byte[] content, String contentType, String fileName);
}

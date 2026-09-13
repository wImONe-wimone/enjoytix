package com.wimone.enjoytix.agent.knowledge.chunk;

import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeChunk;
import com.wimone.enjoytix.agent.knowledge.parser.ParsedDocument;

import java.util.List;

public interface TextChunker {
    List<KnowledgeChunk> chunk(String versionId, String sourceName, ParsedDocument document);
}

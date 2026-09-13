package com.wimone.enjoytix.agent.knowledge.embedding;

import java.util.List;

public interface EmbeddingPort {
    List<float[]> embed(List<String> texts);
}

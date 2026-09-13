package com.wimone.enjoytix.agent.knowledge.chunk;

import com.wimone.enjoytix.agent.knowledge.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FixedSizeTextChunkerTest {
    private final FixedSizeTextChunker chunker = new FixedSizeTextChunker(new TextChunkProperties(10, 2));

    @Test
    void createsOrderedOverlappingChunksWithStableHashes() {
        ParsedDocument document = new ParsedDocument("abcdefghij klmnopqrst uvwxyz", Map.of("title", "Policy"));

        var first = chunker.chunk("doc-1:v1", "policy.txt", document);
        var second = chunker.chunk("doc-1:v1", "policy.txt", document);

        assertThat(first).isNotEmpty();
        assertThat(first).extracting(item -> item.ordinal()).containsExactly(0, 1, 2, 3);
        assertThat(first).extracting(item -> item.id()).containsExactly(
                "doc-1:v1:0", "doc-1:v1:1", "doc-1:v1:2", "doc-1:v1:3");
        assertThat(first).extracting(item -> item.contentHash()).containsExactlyElementsOf(
                second.stream().map(item -> item.contentHash()).toList());
        assertThat(first.get(0).metadata()).containsEntry("title", "Policy");
    }

    @Test
    void keepsParagraphBoundaryWhenThereIsEnoughRoom() {
        ParsedDocument document = new ParsedDocument("first line\nsecond line\nthird line", Map.of());

        var chunks = new FixedSizeTextChunker(new TextChunkProperties(24, 0))
                .chunk("doc-1:v1", "policy.txt", document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content()).isEqualTo("first line\nsecond line");
    }
}

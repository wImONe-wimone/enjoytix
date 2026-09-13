package com.wimone.enjoytix.agent.knowledge.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TikaDocumentParserTest {
    private final DocumentParser parser = new TikaDocumentParser();

    @Test
    void extractsPlainTextAndMetadata() {
        ParsedDocument result = parser.parse("Ticket policy\nRefund is unavailable.".getBytes(StandardCharsets.UTF_8),
                "text/plain", "policy.txt");

        assertThat(result.text()).contains("Ticket policy", "Refund is unavailable");
        assertThat(result.metadata()).containsEntry("contentType", "text/plain");
    }

    @Test
    void rejectsEmptyContent() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> parser.parse(new byte[0], "text/plain", "empty.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

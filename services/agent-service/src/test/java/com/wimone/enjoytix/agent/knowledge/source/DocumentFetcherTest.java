package com.wimone.enjoytix.agent.knowledge.source;

import com.wimone.enjoytix.agent.knowledge.domain.SourceType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentFetcherTest {

    private final KnowledgeSourceProperties properties = new KnowledgeSourceProperties(
            64, Duration.ofSeconds(1), Duration.ofSeconds(1),
            Set.of("text/plain", "text/markdown", "application/pdf"));

    @Test
    void uploadPreservesMetadataAndCalculatesChecksum() {
        UploadDocumentFetcher fetcher = new UploadDocumentFetcher(properties);

        DocumentFetcher.FetchResult result = fetcher.fetch(DocumentFetcher.DocumentFetchRequest.upload(
                "policy.md", "text/markdown",
                new ByteArrayInputStream("ticket policy".getBytes(StandardCharsets.UTF_8))));

        assertThat(result.fileName()).isEqualTo("policy.md");
        assertThat(result.contentType()).isEqualTo("text/markdown");
        assertThat(result.contentLength()).isEqualTo(13);
        assertThat(result.checksum()).isEqualTo("43c796a86f94425ad5c9319e77d9bfe2164ddca395e1be29ff44807239c6f320");
        assertThat(result.content()).isEqualTo("ticket policy".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadRejectsContentLargerThanConfiguredLimit() {
        UploadDocumentFetcher fetcher = new UploadDocumentFetcher(properties);

        assertThatThrownBy(() -> fetcher.fetch(DocumentFetcher.DocumentFetchRequest.upload(
                "policy.txt", "text/plain", new ByteArrayInputStream(new byte[65]))))
                .isInstanceOf(DocumentFetcher.FetchException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void httpFetcherRejectsPrivateResolvedAddressBeforeSendingRequest() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpDocumentFetcher fetcher = new HttpDocumentFetcher(properties, client,
                host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")});

        assertThatThrownBy(() -> fetcher.fetch(DocumentFetcher.DocumentFetchRequest.url("http://example.com/policy.md")))
                .isInstanceOf(DocumentFetcher.FetchException.class)
                .hasMessageContaining("private or local");
        verifyNoInteractions(client);
    }

    @Test
    void httpFetcherReadsBoundedResponseAndUsesUrlFilename() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = "remote policy".getBytes(StandardCharsets.UTF_8);
        server.createContext("/policy.md", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/markdown; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            HttpDocumentFetcher fetcher = new HttpDocumentFetcher(properties,
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                    host -> new InetAddress[]{InetAddress.getByName("93.184.216.34")});

            DocumentFetcher.FetchResult result = fetcher.fetch(DocumentFetcher.DocumentFetchRequest.url(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/policy.md"));

            assertThat(result.fileName()).isEqualTo("policy.md");
            assertThat(result.contentType()).isEqualTo("text/markdown");
            assertThat(result.content()).isEqualTo(body);
            assertThat(result.contentLength()).isEqualTo(body.length);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestTypeIsExplicitlyBoundToFetcher() {
        UploadDocumentFetcher fetcher = new UploadDocumentFetcher(properties);

        assertThatThrownBy(() -> fetcher.fetch(DocumentFetcher.DocumentFetchRequest.url("https://example.com/policy.md")))
                .isInstanceOf(DocumentFetcher.FetchException.class)
                .hasMessageContaining(SourceType.UPLOAD.code());
    }
}

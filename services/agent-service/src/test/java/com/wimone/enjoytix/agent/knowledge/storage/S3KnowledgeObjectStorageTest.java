package com.wimone.enjoytix.agent.knowledge.storage;

import com.wimone.enjoytix.agent.knowledge.source.DocumentFetcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3KnowledgeObjectStorageTest {

    private final S3Client client = mock(S3Client.class);
    private final KnowledgeStorageProperties properties = new KnowledgeStorageProperties(
            true, "http://rustfs.local", "us-east-1", "knowledge", "access", "secret", true, 1024);
    private final S3KnowledgeObjectStorage storage = new S3KnowledgeObjectStorage(client, properties);

    @Test
    void objectKeyIsDeterministicAndNormalizesChecksum() {
        assertThat(storage.objectKey("kb-1", "doc-1", 2, "ABCDEF"))
                .isEqualTo("knowledge/kb-1/doc-1/v2/abcdef");
    }

    @Test
    void putPreservesContentMetadataWithoutLoggingSecrets() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        DocumentFetcher.FetchResult document = new DocumentFetcher.FetchResult(
                "policy.md", "text/markdown", 6, "abcdef", "policy".getBytes(StandardCharsets.UTF_8));

        storage.put("knowledge/kb-1/doc-1/v2/abcdef", document);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("knowledge");
        assertThat(request.key()).isEqualTo("knowledge/kb-1/doc-1/v2/abcdef");
        assertThat(request.contentType()).isEqualTo("text/markdown");
        assertThat(request.contentLength()).isEqualTo(6);
        assertThat(request.metadata()).containsEntry("checksum", "abcdef")
                .containsEntry("original-filename", "policy.md")
                .containsEntry("content-length", "6");
        assertThat(properties.toString()).doesNotContain("secret");
    }

    @Test
    void existsReturnsFalseForMissingObject() {
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertThat(storage.exists("knowledge/kb-1/doc-1/v2/abcdef")).isFalse();
    }

    @Test
    void objectKeyRejectsPathTraversal() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                storage.objectKey("kb/../private", "doc-1", 1, "abcdef")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.wimone.enjoytix.agent.knowledge.storage;

import com.wimone.enjoytix.agent.knowledge.source.DocumentFetcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "agent.knowledge.storage", name = "enabled", havingValue = "true")
public class S3KnowledgeObjectStorage implements KnowledgeObjectStorage {
    private final S3Client client;
    private final KnowledgeStorageProperties properties;

    public S3KnowledgeObjectStorage(S3Client client, KnowledgeStorageProperties properties) { this.client=client; this.properties=properties; }

    @Override public String objectKey(String knowledgeBaseId, String documentId, int version, String checksum) {
        validSegment(knowledgeBaseId); validSegment(documentId); validSegment(checksum);
        if (version <= 0) throw new IllegalArgumentException("version must be positive");
        return "knowledge/" + knowledgeBaseId + "/" + documentId + "/v" + version + "/" + checksum.toLowerCase(java.util.Locale.ROOT);
    }

    @Override public void put(String key, DocumentFetcher.FetchResult document) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("object key is required");
        if (document.contentLength() > properties.maxBytes()) throw new IllegalArgumentException("maximum size exceeded");
        client.putObject(PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(document.contentType()).contentLength(document.contentLength()).metadata(Map.of("checksum", document.checksum(), "original-filename", document.fileName(), "content-length", Long.toString(document.contentLength()))).build(), RequestBody.fromBytes(document.content()));
    }

    @Override public boolean exists(String key) {
        try { client.headObject(HeadObjectRequest.builder().bucket(properties.bucket()).key(key).build()); return true; }
        catch (NoSuchKeyException exception) { return false; }
        catch (S3Exception exception) { if (exception.statusCode() == 404) return false; throw exception; }
    }

    private void validSegment(String value) { if (value == null || value.isBlank() || value.contains("/") || value.contains("\\") || value.contains("..")) throw new IllegalArgumentException("object key segment is invalid"); }
}

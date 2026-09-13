package com.wimone.enjoytix.agent.knowledge.source;

import com.wimone.enjoytix.agent.knowledge.domain.SourceType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class UploadDocumentFetcher implements DocumentFetcher {
    private final KnowledgeSourceProperties properties;

    public UploadDocumentFetcher(KnowledgeSourceProperties properties) { this.properties = properties; }

    @Override public SourceType supportedType() { return SourceType.UPLOAD; }

    @Override
    public FetchResult fetch(DocumentFetchRequest request) {
        requireType(request);
        if (request.fileName() == null || request.fileName().isBlank()) throw new FetchException("file name is required");
        String contentType = normalizeType(request.contentType());
        if (!properties.allowedContentTypes().contains(contentType)) throw new FetchException("content type is not allowed: " + contentType);
        if (request.content() == null) throw new FetchException("upload content is required");
        return read(request.content(), request.fileName(), contentType);
    }

    static String normalizeType(String value) {
        if (value == null || value.isBlank()) throw new FetchException("content type is required");
        return value.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
    }

    static FetchResult read(InputStream input, String fileName, String contentType, long maxBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) throw new FetchException("maximum size exceeded: " + maxBytes + " bytes");
                output.write(buffer, 0, count);
                digest.update(buffer, 0, count);
            }
            return new FetchResult(fileName, contentType, total, hex(digest.digest()), output.toByteArray());
        } catch (IOException exception) {
            throw new FetchException("cannot read upload content", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private FetchResult read(InputStream input, String fileName, String contentType) {
        return read(input, fileName, contentType, properties.maxBytes());
    }

    private void requireType(DocumentFetchRequest request) {
        if (request == null || request.type() != supportedType()) throw new FetchException("request type must be " + supportedType().code());
    }

    static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}

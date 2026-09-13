package com.wimone.enjoytix.agent.knowledge.source;

import com.wimone.enjoytix.agent.knowledge.domain.SourceType;

import java.io.InputStream;

public interface DocumentFetcher {
    SourceType supportedType();
    FetchResult fetch(DocumentFetchRequest request);

    record DocumentFetchRequest(SourceType type, String fileName, String contentType,
                                InputStream content, String location) {
        public static DocumentFetchRequest upload(String fileName, String contentType, InputStream content) {
            return new DocumentFetchRequest(SourceType.UPLOAD, fileName, contentType, content, null);
        }
        public static DocumentFetchRequest url(String location) {
            return new DocumentFetchRequest(SourceType.URL, null, null, null, location);
        }
    }

    record FetchResult(String fileName, String contentType, long contentLength, String checksum, byte[] content) {
        public FetchResult {
            if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("file name is required");
            if (contentType == null || contentType.isBlank()) throw new IllegalArgumentException("content type is required");
            if (contentLength < 0 || content == null || content.length != contentLength) throw new IllegalArgumentException("content length is invalid");
            if (checksum == null || checksum.isBlank()) throw new IllegalArgumentException("checksum is required");
        }
    }

    class FetchException extends RuntimeException {
        public FetchException(String message) { super(message); }
        public FetchException(String message, Throwable cause) { super(message, cause); }
    }
}

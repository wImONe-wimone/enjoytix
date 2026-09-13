package com.wimone.enjoytix.agent.knowledge.source;

import com.wimone.enjoytix.agent.knowledge.domain.SourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpDocumentFetcher implements DocumentFetcher {
    private final KnowledgeSourceProperties properties;
    private final HttpClient httpClient;
    private final HostResolver resolver;

    @Autowired
    public HttpDocumentFetcher(KnowledgeSourceProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).followRedirects(HttpClient.Redirect.NEVER).build(), InetAddress::getAllByName);
    }

    public HttpDocumentFetcher(KnowledgeSourceProperties properties, HttpClient httpClient, HostResolver resolver) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.resolver = resolver;
    }

    @Override public SourceType supportedType() { return SourceType.URL; }

    @Override
    public FetchResult fetch(DocumentFetchRequest request) {
        if (request == null || request.type() != supportedType()) throw new FetchException("request type must be " + supportedType().code());
        URI uri;
        try { uri = URI.create(request.location()); } catch (IllegalArgumentException exception) { throw new FetchException("URL is invalid", exception); }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new FetchException("URL scheme is not allowed");
        validateHost(uri.getHost());
        try {
            HttpResponse<byte[]> response = httpClient.send(HttpRequest.newBuilder(uri).timeout(properties.readTimeout()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new FetchException("remote document returned HTTP " + response.statusCode());
            if (response.body().length > properties.maxBytes()) throw new FetchException("maximum size exceeded: " + properties.maxBytes() + " bytes");
            String contentType = UploadDocumentFetcher.normalizeType(response.headers().firstValue("Content-Type").orElse("application/octet-stream"));
            if (!properties.allowedContentTypes().contains(contentType)) throw new FetchException("content type is not allowed: " + contentType);
            String fileName = fileName(uri);
            return UploadDocumentFetcher.read(new java.io.ByteArrayInputStream(response.body()), fileName, contentType, properties.maxBytes());
        } catch (FetchException exception) { throw exception; }
        catch (Exception exception) { throw new FetchException("cannot fetch remote document", exception); }
    }

    private void validateHost(String host) {
        if (host == null || host.isBlank()) throw new FetchException("URL host is required");
        try {
            for (InetAddress address : resolver.resolve(host)) if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) throw new FetchException("private or local address is not allowed");
        } catch (FetchException exception) { throw exception; }
        catch (Exception exception) { throw new FetchException("URL host cannot be resolved", exception); }
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) return "remote-document";
        String value = path.substring(path.lastIndexOf('/') + 1);
        return value.isBlank() ? "remote-document" : value;
    }

    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws Exception;
    }
}

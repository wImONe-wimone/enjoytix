package com.wimone.enjoytix.agent.knowledge.parser;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TikaDocumentParser implements DocumentParser {
    private final AutoDetectParser parser = new AutoDetectParser(TikaConfig.getDefaultConfig());

    @Override
    public ParsedDocument parse(byte[] content, String contentType, String fileName) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("document content is required");
        Metadata metadata = new Metadata();
        if (contentType != null && !contentType.isBlank()) metadata.set(Metadata.CONTENT_TYPE, contentType);
        if (fileName != null && !fileName.isBlank()) metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        BodyContentHandler handler = new BodyContentHandler(-1);
        try (TikaInputStream input = TikaInputStream.get(new ByteArrayInputStream(content))) {
            parser.parse(input, handler, metadata, new org.apache.tika.parser.ParseContext());
            Map<String, String> values = new LinkedHashMap<>();
            String detectedType = metadata.get(Metadata.CONTENT_TYPE);
            if (detectedType != null) values.put("contentType", detectedType.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT));
            String title = metadata.get(TikaCoreProperties.TITLE);
            if (title != null && !title.isBlank()) values.put("title", title);
            return new ParsedDocument(handler.toString().trim(), values);
        } catch (Exception exception) {
            throw new IllegalArgumentException("document parsing failed", exception);
        }
    }
}

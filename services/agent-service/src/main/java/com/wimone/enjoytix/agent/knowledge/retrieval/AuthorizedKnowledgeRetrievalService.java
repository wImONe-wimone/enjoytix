package com.wimone.enjoytix.agent.knowledge.retrieval;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.observability.KnowledgeRetrievalObservation;
import com.wimone.enjoytix.agent.knowledge.observability.KnowledgeRetrievalObserver;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AuthorizedKnowledgeRetrievalService {
    private final VectorStore vectorStore;
    private final KnowledgeRetrievalAuthorization authorization;
    private final KnowledgeReranker reranker;
    private final RagProperties properties;
    private final KnowledgeCitationAssembler citationAssembler;
    private final KnowledgeRetrievalObserver observer;

    public AuthorizedKnowledgeRetrievalService(VectorStore vectorStore,
                                               KnowledgeRetrievalAuthorization authorization,
                                               KnowledgeReranker reranker,
                                               RagProperties properties) {
        this(vectorStore, authorization, reranker, properties, observation -> { });
    }

    public AuthorizedKnowledgeRetrievalService(VectorStore vectorStore,
                                               KnowledgeRetrievalAuthorization authorization,
                                               KnowledgeReranker reranker,
                                               RagProperties properties,
                                               KnowledgeRetrievalObserver observer) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vector store is required");
        this.authorization = Objects.requireNonNull(authorization, "retrieval authorization is required");
        this.reranker = Objects.requireNonNull(reranker, "reranker is required");
        this.properties = Objects.requireNonNull(properties, "RAG properties are required");
        this.citationAssembler = new KnowledgeCitationAssembler();
        this.observer = Objects.requireNonNull(observer, "retrieval observer is required");
    }

    public KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request) {
        long started = System.nanoTime();
        Set<String> scopes = authorization.authorizedKnowledgeBases(request.user());
        if (scopes == null || scopes.isEmpty() || !properties.isRetrievalEnabled()) {
            return observed(KnowledgeRetrievalResult.noHit(), 0, 0, started);
        }
        int candidateLimit = Math.max(1, Math.min(properties.getCandidateLimit(), 50));
        List<Document> candidates = new ArrayList<>();
        try {
            for (String scope : scopes) {
                if (scope == null || scope.isBlank() || candidates.size() >= candidateLimit) continue;
                int remaining = candidateLimit - candidates.size();
                SearchRequest searchRequest = SearchRequest.builder().query(request.query()).topK(remaining)
                        .similarityThreshold(request.minimumScore())
                        .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ,
                                new Filter.Key("knowledgeBaseId"), new Filter.Value(scope))).build();
                candidates.addAll(vectorStore.similaritySearch(searchRequest));
            }
        } catch (RuntimeException failure) {
            return observed(KnowledgeRetrievalResult.failure(failure, "VECTOR_STORE"), candidates.size(), 0, started);
        }
        List<Document> authorized = candidates.stream().filter(document -> isAuthorized(document, scopes))
                .sorted(Comparator.comparing(Document::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(candidateLimit).toList();
        if (authorized.isEmpty()) {
            return observed(KnowledgeRetrievalResult.noHit(), 0, 0, started);
        }
        List<Document> finalDocuments = authorized;
        if (properties.isRerankingEnabled()) {
            try {
                finalDocuments = reranker.rerank(request.query(), authorized);
            } catch (RuntimeException failure) {
                return observed(KnowledgeRetrievalResult.failure(failure, "RERANKER"), authorized.size(), 0, started);
            }
        }
        List<KnowledgeCitation> citations = citationAssembler.assemble(finalDocuments.stream()
                .limit(Math.min(request.topK(), properties.getTopK())).toList(), scopes);
        KnowledgeRetrievalResult result = citations.isEmpty()
                ? KnowledgeRetrievalResult.noHit()
                : KnowledgeRetrievalResult.success(citations,
                Map.of("candidateCount", String.valueOf(authorized.size()),
                        "finalCount", String.valueOf(citations.size())));
        return observed(result, authorized.size(), citations.size(), started);
    }

    private KnowledgeRetrievalResult observed(KnowledgeRetrievalResult result, int candidates,
                                              int finals, long started) {
        observer.record(new KnowledgeRetrievalObservation(result.outcome(), candidates, finals,
                finals == 0 ? 0 : 1,
                Math.max(0, (System.nanoTime() - started) / 1_000_000), null, null));
        return result;
    }

    private boolean isAuthorized(Document document, Set<String> scopes) {
        Object scope = document.getMetadata().get("knowledgeBaseId");
        Object active = document.getMetadata().get("active");
        return scope != null && scopes.contains(scope.toString())
                && (active == null || Boolean.parseBoolean(active.toString()))
                && document.getMetadata().get("versionId") != null
                && document.getMetadata().get("chunkId") != null;
    }
}
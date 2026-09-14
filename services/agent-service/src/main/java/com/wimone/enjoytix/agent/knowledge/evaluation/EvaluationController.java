package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

@RestController
@Profile("evaluation")
@ConditionalOnProperty(prefix = "agent.knowledge.evaluation", name = "enabled", havingValue = "true")
@ConditionalOnBean(EvaluationRetrievalAdapter.class)
@RequestMapping("/internal/agent/evaluation")
public class EvaluationController {
    static final String TRUSTED_TOKEN_HEADER = "X-Agent-Evaluation-Token";

    private final EvaluationRetrievalAdapter adapter;
    private final String trustedToken;

    public EvaluationController(EvaluationRetrievalAdapter adapter, EvaluationProperties properties) {
        this.adapter = Objects.requireNonNull(adapter, "evaluation adapter is required");
        this.trustedToken = Objects.requireNonNull(properties, "evaluation properties are required").getTrustedToken();
    }

    @PostMapping("/retrieval")
    public Result<List<EvaluationEvidenceRecord>> retrieve(
            HttpServletRequest request, @Valid @RequestBody EvaluationRetrievalRequest retrievalRequest) {
        requireTrustedCredential(request);
        return Results.success(adapter.evaluate(retrievalRequest.datasetId(), retrievalRequest.caseIds()));
    }

    private void requireTrustedCredential(HttpServletRequest request) {
        String presentedToken = request == null ? null : request.getHeader(TRUSTED_TOKEN_HEADER);
        if (trustedToken.isBlank() || presentedToken == null || presentedToken.isBlank()
                || !MessageDigest.isEqual(trustedToken.getBytes(StandardCharsets.UTF_8),
                presentedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "trusted evaluation credential is required");
        }
    }
}

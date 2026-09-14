package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EvaluationControllerTest {
    @Test
    void acceptsOnlyRegisteredIdentifiersWithTrustedCredential() {
        EvaluationRetrievalAdapter adapter = mock(EvaluationRetrievalAdapter.class);
        EvaluationEvidenceRecord evidence = evidence();
        when(adapter.evaluate("baseline", List.of("case-1"))).thenReturn(List.of(evidence));
        EvaluationProperties properties = new EvaluationProperties();
        properties.setTrustedToken("trusted-token");
        EvaluationController controller = new EvaluationController(adapter, properties);
        MockHttpServletRequest request = requestWithToken("trusted-token");

        var result = controller.retrieve(request, new EvaluationRetrievalRequest("baseline", List.of("case-1")));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly(evidence);
        verify(adapter).evaluate("baseline", List.of("case-1"));
    }

    @Test
    void rejectsMissingOrInvalidTrustedCredentialBeforeRetrieval() {
        EvaluationRetrievalAdapter adapter = mock(EvaluationRetrievalAdapter.class);
        EvaluationProperties properties = new EvaluationProperties();
        properties.setTrustedToken("trusted-token");
        EvaluationController controller = new EvaluationController(adapter, properties);

        assertThatThrownBy(() -> controller.retrieve(new MockHttpServletRequest(),
                new EvaluationRetrievalRequest("baseline", List.of("case-1"))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(401);
        assertThatThrownBy(() -> controller.retrieve(requestWithToken("wrong-token"),
                new EvaluationRetrievalRequest("baseline", List.of("case-1"))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(401);

        verifyNoInteractions(adapter);
    }

    @Test
    void rejectsCallerSuppliedRetrievalOverridesDuringJsonBinding() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> objectMapper.readValue(
                "{\"datasetId\":\"baseline\",\"caseIds\":[\"case-1\"],\"query\":\"override\"}",
                EvaluationRetrievalRequest.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(EvaluationController.TRUSTED_TOKEN_HEADER, token);
        return request;
    }

    private EvaluationEvidenceRecord evidence() {
        return new EvaluationEvidenceRecord(
                "1", "baseline", "baseline-1", "case-1", KnowledgeRetrievalOutcome.NO_HIT,
                List.of(), List.of(), 0, 0, 1,
                new EvaluationRetrievalConfiguration(true, false, 5, 20, 0.65,
                        "text-embedding-v3", 1024),
                new EvaluationDeterministicResult("1", 1, 1, 1, 1, true), null);
    }
}

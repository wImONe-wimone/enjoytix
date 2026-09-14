package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EvaluationEvidenceRecordTest {
    @Test
    void retainsVersionedOrderedEvidenceForSuccessfulRetrieval() {
        EvaluationEvidenceCitation firstCitation = new EvaluationEvidenceCitation(
                "citation-1", "kb-public", "document-1", "version-2", "chunk-1", 1, 0.91);
        EvaluationEvidenceCitation secondCitation = new EvaluationEvidenceCitation(
                "citation-2", "kb-public", "document-2", "version-3", "chunk-7", 2, 0.82);
        EvaluationEvidenceRecord evidence = new EvaluationEvidenceRecord(
                "1", "baseline", "baseline-1", "case-1", KnowledgeRetrievalOutcome.SUCCESS,
                List.of(firstCitation, secondCitation),
                List.of(new EvaluationEvidenceContext("citation-1", "First retrieved context.")),
                4, 2, 17, new EvaluationRetrievalConfiguration(true, false, 5, 20, 0.65,
                        "text-embedding-v3", 1024),
                new EvaluationDeterministicResult("1", 1.0, 1.0, 1.0, 1.0, true), null);

        assertThat(evidence.contractVersion()).isEqualTo("1");
        assertThat(evidence.citations()).extracting(EvaluationEvidenceCitation::versionId)
                .containsExactly("version-2", "version-3");
        assertThat(evidence.contexts()).extracting(EvaluationEvidenceContext::content)
                .containsExactly("First retrieved context.");
        assertThat(evidence.deterministicResult().thresholdsMet()).isTrue();
    }

    @Test
    void rejectsNoHitEvidenceThatContainsCitationsOrContexts() {
        EvaluationEvidenceCitation citation = new EvaluationEvidenceCitation(
                "citation-1", "kb-public", "document-1", "version-2", "chunk-1", 1, 0.91);

        assertThatIllegalArgumentException().isThrownBy(() -> new EvaluationEvidenceRecord(
                "1", "baseline", "baseline-1", "case-1", KnowledgeRetrievalOutcome.NO_HIT,
                List.of(citation), List.of(), 0, 0, 5,
                new EvaluationRetrievalConfiguration(true, false, 5, 20, 0.65,
                        "text-embedding-v3", 1024),
                new EvaluationDeterministicResult("1", 1.0, 1.0, 1.0, 1.0, true), null))
                .withMessageContaining("no-hit evidence");
    }
}

package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EvaluationDatasetLoaderTest {
    private final EvaluationDatasetLoader loader = new EvaluationDatasetLoader();

    @Test
    void loadsVersionedJsonlAndRejectsDuplicateCaseIds() throws Exception {
        Path file = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(file, """
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"positive","query":"refund policy","subjectProfileId":"public-user","expectedOutcome":"SUCCESS","expectedCitationKeys":["cite-1"],"referenceAnswer":"Refunds are available.","tags":["positive"]}
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"positive","query":"duplicate","subjectProfileId":"public-user","expectedOutcome":"SUCCESS","expectedCitationKeys":["cite-1"],"referenceAnswer":"Duplicate.","tags":["duplicate"]}
                """);

        assertThatIllegalArgumentException().isThrownBy(() -> loader.load(file, "baseline"))
                .withMessageContaining("duplicate caseId");
    }

    @Test
    void loadsAValidDatasetWithStableVersions() throws Exception {
        Path file = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(file, """
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"positive","query":"refund policy","subjectProfileId":"public-user","expectedOutcome":"SUCCESS","expectedCitationKeys":["cite-1"],"referenceAnswer":"Refunds are available.","tags":["positive"]}
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"no-hit","query":"unknown policy","subjectProfileId":"public-user","expectedOutcome":"NO_HIT","expectedCitationKeys":[],"referenceAnswer":null,"tags":["no-hit"]}
                """);

        EvaluationDataset dataset = loader.load(file, "baseline");

        assertThat(dataset.datasetId()).isEqualTo("baseline");
        assertThat(dataset.datasetVersion()).isEqualTo("baseline-1");
        assertThat(dataset.cases()).extracting(RetrievalEvaluationCase::caseId)
                .containsExactly("positive", "no-hit");
    }

    @Test
    void rejectsUnknownSubjectProfilesBeforeRetrievalCanRun() throws Exception {
        Path file = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(file, """
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"unknown-subject","query":"refund policy","subjectProfileId":"unregistered-user","expectedOutcome":"SUCCESS","expectedCitationKeys":["cite-1"],"referenceAnswer":"Refunds are available.","tags":["positive"]}
                """);
        EvaluationSubjectProfileRegistry profiles = new EvaluationSubjectProfileRegistry(Map.of(
                "public-user", new EvaluationSubjectProfile("public-user", new AgentUserContext(7L, "alice"))));

        assertThatIllegalArgumentException().isThrownBy(() -> loader.load(file, "baseline", profiles))
                .withMessageContaining("unknown evaluation subject profile");
    }

    @Test
    void rejectsMalformedJsonAndInconsistentDatasetVersions() throws Exception {
        Path malformedFile = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(malformedFile, "{not-json}");
        Path inconsistentFile = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(inconsistentFile, """
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"positive","query":"refund policy","subjectProfileId":"public-user","expectedOutcome":"SUCCESS","expectedCitationKeys":["cite-1"],"referenceAnswer":"Refunds are available.","tags":["positive"]}
                {"schemaVersion":"1","datasetVersion":"baseline-2","caseId":"no-hit","query":"unknown policy","subjectProfileId":"public-user","expectedOutcome":"NO_HIT","expectedCitationKeys":[],"referenceAnswer":null,"tags":["no-hit"]}
                """);

        assertThatIllegalArgumentException().isThrownBy(() -> loader.load(malformedFile, "baseline"))
                .withMessageContaining("invalid dataset JSON at line 1");
        assertThatIllegalArgumentException().isThrownBy(() -> loader.load(inconsistentFile, "baseline"))
                .withMessageContaining("inconsistent dataset version");
    }

    @Test
    void rejectsNoHitCasesWithExpectedCitations() throws Exception {
        Path file = Files.createTempFile("rag-evaluation", ".jsonl");
        Files.writeString(file, """
                {"schemaVersion":"1","datasetVersion":"baseline-1","caseId":"no-hit","query":"unknown policy","subjectProfileId":"public-user","expectedOutcome":"NO_HIT","expectedCitationKeys":["cite-1"],"referenceAnswer":null,"tags":["no-hit"]}
                """);

        assertThatIllegalArgumentException().isThrownBy(() -> loader.load(file, "baseline"))
                .withMessageContaining("invalid dataset JSON at line 1");
    }

    @Test
    void loadsBaselineFixtureWithEveryRequiredSafetyCase() {
        EvaluationSubjectProfileRegistry profiles = new EvaluationSubjectProfileRegistry(Map.of(
                "public-user", new EvaluationSubjectProfile("public-user", new AgentUserContext(7L, "alice"))));

        EvaluationDataset dataset = loader.load(
                new ClassPathResource("evaluation/rag-retrieval/datasets/baseline.jsonl"), "baseline", profiles);

        assertThat(dataset.cases()).hasSize(5);
        assertThat(dataset.cases()).filteredOn(item -> item.tags().contains("positive")).hasSize(1);
        assertThat(dataset.cases()).filteredOn(item -> item.tags().contains("no-hit")).hasSize(1);
        assertThat(dataset.cases()).filteredOn(item -> item.tags().contains("authorization-isolation")).hasSize(1);
        assertThat(dataset.cases()).filteredOn(item -> item.tags().contains("retired-version")).hasSize(1);
        assertThat(dataset.cases()).filteredOn(item -> item.tags().contains("near-match")).hasSize(1);
    }
}

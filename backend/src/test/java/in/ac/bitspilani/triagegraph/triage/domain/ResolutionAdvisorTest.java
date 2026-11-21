package in.ac.bitspilani.triagegraph.triage.domain;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.archived;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.atCosine;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;
import in.ac.bitspilani.triagegraph.triage.TriageFixtures.FakeLlm;
import in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex;

class ResolutionAdvisorTest {

    private InMemoryVectorIndex index;
    private HistoricalResolutionAdvisor historical;
    private FakeLlm llm;

    @BeforeEach
    void setUp() {
        index = new InMemoryVectorIndex();
        index.put(archived(1, "HIST-002", "APPLICATION", "Fee Payment App", "Renewed the expired TLS certificate."), atCosine(0.88));
        index.put(archived(2, "HIST-001", "APPLICATION", "Fee Payment App", "Rolled back the release."), atCosine(0.60));
        index.put(archived(3, "HIST-040", "HARDWARE", "Print Services", "Replaced rollers."), atCosine(0.10));
        historical = new HistoricalResolutionAdvisor(index, 0.30);
        llm = new FakeLlm();
    }

    private static ResolutionQuery request(boolean useAi) {
        return new ResolutionQuery("Payment gateway timeout, call 9876543210", query(), null, 3, useAi);
    }

    @Test
    void historicalAdvisorReturnsSimilarFixesAndDraftsFromTheClosest() {
        var s = historical.advise(request(false));
        assertEquals(2, s.basedOn().size());                     // the printer fix is below 0.30
        assertEquals("HIST-002", s.basedOn().get(0).code());
        assertEquals(-1L, s.basedOn().get(0).incidentId());      // archive entries have negative ids
        assertTrue(s.draft().startsWith("Closest past fix (HIST-002, 88% similar): Renewed the expired TLS certificate."));
        assertFalse(s.aiGenerated());
    }

    @Test
    void llmDecoratorTurnsRetrievedFixesIntoStepsWhenAllowed() {
        llm.text = "1. Check the certificate (HIST-002)\n2. Restart the connector";
        var s = new LlmResolutionAdvisor(historical, llm, () -> true).advise(request(true));
        assertTrue(s.aiGenerated());
        assertEquals(llm.text, s.draft());
        String prompt = llm.prompts.get(0).user();
        assertTrue(prompt.contains("HIST-002") && prompt.contains("[phone]") && !prompt.contains("9876543210"));
    }

    @Test
    void decoratorFallsBackToTheHistoricalDraftWhenAiIsOffOrFails() {
        assertFalse(new LlmResolutionAdvisor(historical, llm, () -> false).advise(request(true)).aiGenerated());
        assertFalse(new LlmResolutionAdvisor(historical, llm, () -> true).advise(request(false)).aiGenerated());
        llm.failure = new LlmUnavailableException("quota");
        var s = new LlmResolutionAdvisor(historical, llm, () -> true).advise(request(true));
        assertFalse(s.aiGenerated());
        assertTrue(s.draft().startsWith("Closest past fix"));
    }

    @Test
    void nothingSimilarGivesAnHonestEmptyAnswer() {
        var s = new HistoricalResolutionAdvisor(new InMemoryVectorIndex(), 0.30).advise(request(false));
        assertTrue(s.basedOn().isEmpty());
        assertEquals("No similar resolved incident found yet.", s.draft());
    }
}

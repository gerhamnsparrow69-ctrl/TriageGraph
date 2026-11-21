package in.ac.bitspilani.triagegraph.triage.application;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import in.ac.bitspilani.triagegraph.triage.domain.EnsembleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.ClassificationCandidate;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;

/** The triage facade end to end (lexical embedder, in-memory repositories, fake LLM, fixed clock). */
class TriageServiceImplTest {

    private TriageTestRig rig;

    @BeforeEach
    void setUp() {
        rig = new TriageTestRig();
        rig.archive("HIST-001", "Fee payment page not loading", "Students see a blank page on the fee payment portal.",
                "APPLICATION", "Fee Payment App", "Restarted the payment app pods and rolled back the release.");
        rig.archive("HIST-002", "Payment gateway timeout after TLS certificate expiry", "Fee payments fail with a gateway timeout.",
                "APPLICATION", "Fee Payment App", "Renewed the expired TLS certificate on the payment connector.");
        rig.archive("HIST-013", "Hostel Wi-Fi down in Block 5", "No Wi-Fi in hostel block 5; phones get no IP address.",
                "NETWORK", "Hostel Wi-Fi", "Replaced the failed PoE switch.");
    }

    private IncidentSnapshot incident(long id, String title, String description, IncidentStatus status, long reporter,
            Duration age) {
        return new IncidentSnapshot(id, "INC-" + id, title, description, status, Priority.P1, 1L, "APPLICATION",
                reporter, Set.of(), 4L, null, NOW.minus(age), null, null);
    }

    @Test
    void previewSuggestsCategoryAndServiceFromKeywordsAndHistory() {
        var insight = rig.triage.analyse("Fee payment app unavailable",
                "Students cannot complete fee transactions; the payment page fails to load.", null, null);
        assertEquals("APPLICATION", insight.suggestion().categoryCode());
        assertEquals(Long.valueOf(1L), insight.suggestion().serviceId());
        assertTrue(insight.suggestion().confidence() >= 0.6, "confidence " + insight.suggestion().confidence());
        assertTrue(insight.classification().votes().size() >= 2, "keywords and kNN both vote");
        assertTrue(insight.duplicates().likely().isEmpty(), "nothing live yet");
    }

    /** Workflow 1b: a second report of the same outage is flagged as a likely duplicate of the live incident. */
    @Test
    void secondReportOfTheSameOutageIsALikelyDuplicate() {
        rig.incidents.replace(incident(1024, "Fee payment app unavailable",
                "Students cannot complete fee transactions; the payment page fails to load since 10 am.",
                IncidentStatus.ASSIGNED, 21, Duration.ofMinutes(4)));
        rig.triage.index(1024, "ignored when a snapshot exists");

        var result = rig.triage.preview(new TriageRequest("Fee payment not going through",
                "The fee payment page will not load so I cannot pay my semester fee.", 22L));
        assertEquals(1, result.likelyDuplicates().size(), "likely: " + result.likelyDuplicates() + " possibly: " + result.possiblyRelated());
        assertEquals("INC-1024", result.likelyDuplicates().get(0).code());
        assertEquals(1024L, result.likelyDuplicates().get(0).incidentId());
    }

    @Test
    void resolvedIncidentsStopBeingDuplicateCandidatesAndTheirFixBecomesKnowledge() {
        rig.incidents.replace(incident(7, "LMS assignment upload fails", "Upload error for big PDFs on the LMS.",
                IncidentStatus.IN_PROGRESS, 21, Duration.ofMinutes(30)));
        rig.triage.index(7, "");
        rig.knowledge.applyStatus(7, IncidentStatus.RESOLVED, "Raised the proxy upload limit to 50 MB.", NOW);

        var preview = rig.triage.preview(new TriageRequest("LMS assignment upload fails", "Upload error for big PDFs.", 30L));
        assertTrue(preview.likelyDuplicates().isEmpty() && preview.possiblyRelated().isEmpty());
        var similar = rig.triage.similarResolved(7, 3);
        assertFalse(similar.stream().anyMatch(s -> s.code().equals("INC-7")), "an incident is never similar to itself");
        var search = rig.triage.searchKnowledge("assignment upload error on LMS for large PDF", false);
        assertEquals("INC-7", search.basedOn().get(0).code());
        assertTrue(search.draft().contains("Raised the proxy upload limit"));
    }

    /** TC22 through the facade. */
    @Test
    void sameReporterSendingTheSameTextAgainIsBlocked() {
        rig.incidents.replace(incident(50, "Projector not working in lab 3", "No signal from any laptop.",
                IncidentStatus.NEW, 21, Duration.ofMinutes(2)));
        rig.triage.index(50, "");
        var e = assertThrows(DuplicateIncidentException.class,
                () -> rig.triage.checkNotRepeatSubmission(21, "Projector not working in lab 3. No signal from any laptop."));
        assertTrue(e.getMessage().contains("INC-50"));
        rig.triage.checkNotRepeatSubmission(22, "Projector not working in lab 3. No signal from any laptop."); // other user: fine
    }

    /** TC19: Gemini failing never breaks triage; the local classifiers still answer. */
    @Test
    void geminiFailureFallsBackToLocalClassifiers() {
        rig.llm.failure = new LlmUnavailableException("network failure");
        var suggestion = rig.triage.classify("Hostel Wi-Fi down in block 5, no IP address");
        assertEquals("NETWORK", suggestion.categoryCode());
        assertNotEquals("LLM", suggestion.source());
        assertEquals(1, rig.llm.prompts.size(), "Gemini was asked once and failed");
    }

    @Test
    void geminiAnswerIsPreferredWhenItIsWithinTheTieMarginOfTheBestLocalVote() {
        rig.llm.json = Map.of("category", "NETWORK", "service", "Hostel Wi-Fi", "confidence", 0.95, "reason", "Wi-Fi outage in a hostel");
        var suggestion = rig.triage.classify("Hostel Wi-Fi down in block 5, no IP address");
        assertEquals("LLM", suggestion.source());               // within 0.10 of the kNN vote, and trusted more
        assertTrue(suggestion.explanation().contains("confirmed by"), suggestion.explanation());
        assertEquals("NETWORK", suggestion.categoryCode());
        assertEquals(Long.valueOf(6L), suggestion.serviceId());
    }

    @Test
    void resolutionSuggestionUsesTheClosestPastFixAndTheAiDraftWhenAvailable() {
        rig.incidents.replace(incident(60, "Payment gateway timeout", "TLS handshake errors to the payment provider.",
                IncidentStatus.IN_PROGRESS, 21, Duration.ofMinutes(10)));
        rig.triage.index(60, "");
        var plain = rig.triage.suggestResolution(60, false);
        assertEquals("HIST-002", plain.basedOn().get(0).code());
        assertFalse(plain.aiGenerated());

        rig.llm.text = "1. Check the connector certificate (HIST-002)";
        var ai = rig.triage.suggestResolution(60, true);
        assertTrue(ai.aiGenerated());
        assertEquals("1. Check the connector certificate (HIST-002)", ai.draft());
    }

    @Test
    void unknownIncidentIsNotFound() {
        assertThrows(IncidentNotFoundException.class, () -> rig.triage.similarResolved(999, 3));
    }

    /** The incident module rejects inactive categories, so a vote for one is replaced by the best active vote. */
    @Test
    void suggestionIsAlwaysAnActiveCatalogCategory() {
        var retired = new ClassificationCandidate("PRINTING", 0.9, null, null, 0, "KNN", "learnt from the archive");
        var active = new ClassificationCandidate("HARDWARE", 0.7, null, null, 0, "KEYWORD", "printer");
        var s = rig.triage.toSuggestion(new EnsembleClassifier.Result(retired, java.util.List.of(retired, active), false));
        assertEquals("HARDWARE", s.categoryCode());
        assertEquals(0.7, s.confidence(), 1e-9);
    }
}

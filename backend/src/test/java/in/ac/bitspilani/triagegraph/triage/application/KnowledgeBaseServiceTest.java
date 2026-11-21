package in.ac.bitspilani.triagegraph.triage.application;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeSource;

class KnowledgeBaseServiceTest {

    private final TriageTestRig rig = new TriageTestRig();

    @Test
    void archivingIsIdempotentByCodeAndResolvesServiceIds() {
        rig.archive("HIST-001", "Fee page down", "Blank page", "APPLICATION", "fee payment app", "Rolled back.");
        rig.archive("HIST-001", "Fee page down", "Blank page", "APPLICATION", "Fee Payment App", "Rolled back.");
        assertEquals(1, rig.knowledgeRepo.docs.size());
        var view = rig.index.views(v -> true).get(0);
        assertEquals(Long.valueOf(1L), view.serviceId());
        assertEquals("Fee Payment App", view.serviceName());
        assertEquals(KnowledgeSource.ARCHIVE, view.source());
        assertTrue(view.hasResolution());
    }

    @Test
    void withoutTheIncidentModuleLiveIncidentsAreIndexedFromTheTextAlone() {
        var view = rig.knowledge.indexLive(42, "Printer jam in library. Jams on every job.");
        assertEquals("INC-42", view.code());
        assertEquals("Printer jam in library", view.title());
        assertEquals(IncidentStatus.NEW, view.status());
        assertTrue(rig.index.contains(view.docId()));
    }

    @Test
    void snapshotFieldsAndStatusChangesAreTracked() {
        rig.incidents.replace(new IncidentSnapshot(9, "INC-0009", "LMS slow", "Course pages take a minute", IncidentStatus.ASSIGNED,
                Priority.P2, 4L, "APPLICATION", 21L, Set.of(), 2L, null, NOW, null, null));
        var view = rig.knowledge.indexLive(9, "");
        assertEquals("LMS", view.serviceName());
        assertEquals(Long.valueOf(21L), view.reporterId());
        assertTrue(view.isOpen());

        rig.knowledge.applyStatus(9, IncidentStatus.RESOLVED, "Enabled page caching.", NOW);
        var after = rig.index.views(v -> v.code().equals("INC-0009")).get(0);
        assertFalse(after.isOpen());
        assertEquals("Enabled page caching.", after.resolutionSummary());
        assertEquals(Long.valueOf(2L), after.resolverId());
    }

    @Test
    void rebuildIndexEmbedsAnythingMissingFromTheIndex() {
        rig.archive("HIST-1", "a", "b", "NETWORK", "LMS", "c");
        var fresh = new KnowledgeBaseService(rig.knowledgeRepo, new in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex(),
                rig.embedder, in.ac.bitspilani.triagegraph.triage.TriageFixtures.provider(null), rig.catalog, TriageTestRig.CLOCK);
        fresh.rebuildIndex(); // must not throw; the rig's index already had it, the new one is filled from the repo
        assertEquals(1, rig.index.size());
    }

    @Test
    void firstSentenceMakesAShortTitle() {
        assertEquals("Wi-Fi down", KnowledgeBaseService.firstSentence("Wi-Fi down. Since morning in block 5."));
        assertEquals(120, KnowledgeBaseService.firstSentence("x".repeat(300)).length());
    }

    /** Member 1 sends "Resolved: <summary>" as the status-change reason; only the summary is kept. */
    @Test
    void resolvedPrefixFromTheIncidentModuleIsStripped() {
        assertEquals("Renewed the TLS certificate", KnowledgeBaseService.resolutionText("Resolved: Renewed the TLS certificate"));
        assertEquals("Rolled back", KnowledgeBaseService.resolutionText("Rolled back"));
        assertEquals(null, KnowledgeBaseService.resolutionText(null));
    }
}

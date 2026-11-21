package in.ac.bitspilani.triagegraph.triage.domain;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.archived;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.atCosine;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.live;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex;

class DuplicateDetectorTest {

    private static final DuplicatePolicy POLICY = new DuplicatePolicy(0.60, 0.50, Duration.ofHours(72), 5);
    private InMemoryVectorIndex index;
    private DuplicateDetector detector;

    @BeforeEach
    void setUp() {
        index = new InMemoryVectorIndex();
        detector = new DuplicateDetector(index);
    }

    /**
     * TC11 boundary: same service, reported just now (recency 1): score = 0.8·cos + 0.2.
     * cos 0.5 → score exactly 0.60 → likely; cos 0.49875 → 0.599 → only possibly related.
     */
    @Test
    void scoreExactlyAtTheThresholdIsALikelyDuplicateAndJustBelowIsOnlyRelated() {
        index.put(live(1, 101, "INC-101", IncidentStatus.ASSIGNED, 7L, "Fee Payment App", NOW, 9L), atCosine(0.5));
        index.put(live(2, 102, "INC-102", IncidentStatus.ASSIGNED, 7L, "Fee Payment App", NOW, 9L), atCosine(0.49875));
        var report = detector.find(query(), 7L, null, NOW, POLICY, null);
        assertEquals(List.of("INC-101"), report.likely().stream().map(m -> m.view().code()).toList());
        assertEquals(0.6, report.likely().get(0).score(), 1e-9);
        assertEquals(List.of("INC-102"), report.possibly().stream().map(m -> m.view().code()).toList());
        assertEquals(0.599, report.possibly().get(0).score(), 1e-9);
    }

    @Test
    void sameServiceAndRecencyEachAddTenPoints() {
        index.put(live(1, 101, "SAME-NEW", IncidentStatus.ASSIGNED, 7L, "Fee Payment App", NOW, 9L), atCosine(0.7));
        index.put(live(2, 102, "OTHER-OLD", IncidentStatus.ASSIGNED, 8L, "LMS", NOW.minus(Duration.ofHours(36)), 9L), atCosine(0.7));
        var report = detector.find(query(), 7L, null, NOW, POLICY, null);
        assertEquals("SAME-NEW", report.likely().get(0).view().code());
        assertEquals(0.76, report.likely().get(0).score(), 1e-3);      // 0.56 + 0.10 + 0.10
        assertEquals(0.61, report.likely().get(1).score(), 1e-3);      // 0.56 + 0 + 0.05
        assertTrue(report.likely().get(0).sameService());
        assertEquals(2160, report.likely().get(1).minutesAgo());
    }

    @Test
    void onlyOpenLiveIncidentsInsideTheWindowAreCandidates() {
        index.put(live(1, 101, "RESOLVED", IncidentStatus.RESOLVED, 7L, null, NOW, 9L), atCosine(0.99));
        index.put(live(2, 102, "MERGED", IncidentStatus.MERGED, 7L, null, NOW, 9L), atCosine(0.99));
        index.put(live(3, 103, "TOO-OLD", IncidentStatus.ASSIGNED, 7L, null, NOW.minus(Duration.ofHours(73)), 9L), atCosine(0.99));
        index.put(archived(4, "HIST-1", "APPLICATION", "Fee Payment App", "fixed"), atCosine(0.99));
        index.put(live(5, 105, "SELF", IncidentStatus.NEW, 7L, null, NOW, 9L), atCosine(0.99));
        var report = detector.find(query(), 7L, null, NOW, POLICY, 105L);
        assertTrue(report.likely().isEmpty() && report.possibly().isEmpty());
    }

    @Test
    void serviceNameMatchesWhenTheCatalogHasNoIds() {
        index.put(live(1, 101, "INC-101", IncidentStatus.NEW, null, "Hostel Wi-Fi", NOW, 9L), atCosine(0.5));
        var report = detector.find(query(), null, "hostel wi-fi", NOW, POLICY, null);
        assertTrue(report.likely().get(0).sameService());
    }

    @Test
    void invalidPoliciesAreRejected() {
        assertThrows(InvalidInputException.class, () -> new DuplicatePolicy(0.5, 0.6, Duration.ofHours(1), 5));
        assertThrows(InvalidInputException.class, () -> new DuplicatePolicy(0.6, 0.5, Duration.ZERO, 5));
        assertThrows(InvalidInputException.class, () -> new DuplicatePolicy(1.0, 0.5, Duration.ofHours(1), 5));
    }

    @Test
    void recencyFallsLinearlyToZeroAtTheEdgeOfTheWindow() {
        assertEquals(1.0, DuplicateDetector.recency(Duration.ZERO, Duration.ofHours(72)), 1e-12);
        assertEquals(0.5, DuplicateDetector.recency(Duration.ofHours(36), Duration.ofHours(72)), 1e-12);
        assertEquals(0.0, DuplicateDetector.recency(Duration.ofHours(100), Duration.ofHours(72)), 1e-12);
    }
}

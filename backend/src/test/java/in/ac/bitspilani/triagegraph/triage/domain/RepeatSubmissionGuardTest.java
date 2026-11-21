package in.ac.bitspilani.triagegraph.triage.domain;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.atCosine;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.live;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.query;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException;
import in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex;

class RepeatSubmissionGuardTest {

    private final InMemoryVectorIndex index = new InMemoryVectorIndex();
    private final RepeatSubmissionGuard guard = new RepeatSubmissionGuard(index);

    /** TC22: same reporter, near-identical text within 10 minutes → 409 pointing at the existing ticket. */
    @Test
    void sameReporterResubmittingWithinTheWindowIsRejected() {
        index.put(live(1, 1024, "INC-1024", IncidentStatus.ASSIGNED, 7L, null, NOW.minus(Duration.ofMinutes(4)), 9L), atCosine(0.97));
        var e = assertThrows(DuplicateIncidentException.class,
                () -> guard.check(9L, query(), NOW, Duration.ofMinutes(10), 0.95));
        assertEquals("You reported this 4 min ago as INC-1024. Please add details to that incident instead.", e.getMessage());
        assertEquals("DUPLICATE_INCIDENT", e.errorCode());
    }

    @Test
    void otherReportersOlderReportsAndDifferentTextArePassed() {
        index.put(live(1, 1, "OTHER", IncidentStatus.NEW, 7L, null, NOW, 8L), atCosine(0.99));
        index.put(live(2, 2, "OLD", IncidentStatus.NEW, 7L, null, NOW.minus(Duration.ofMinutes(11)), 9L), atCosine(0.99));
        index.put(live(3, 3, "DIFFERENT", IncidentStatus.NEW, 7L, null, NOW, 9L), atCosine(0.80));
        assertDoesNotThrow(() -> guard.check(9L, query(), NOW, Duration.ofMinutes(10), 0.95));
    }
}

package in.ac.bitspilani.triagegraph.assignment.application;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures;

class ExperienceServiceTest {

    private final Fakes.FixRepo repo = new Fakes.FixRepo();
    private final ExperienceService experience = new ExperienceService(repo);

    @Test
    void recordingTheSameIncidentTwiceCountsOnce() {
        assertTrue(experience.recordFix(4, 1L, "Fee Payment App", "INC-1024", Instant.EPOCH));
        assertFalse(experience.recordFix(4, 1L, "Fee Payment App", "INC-1024", Instant.EPOCH));
        assertEquals(Map.of(4L, Map.of(1L, 1)), experience.fixesByTechnician());
    }

    @Test
    void enrichAddsLedgerCountsToTheDirectoryCounts() {
        experience.recordFix(4, 1L, null, "A", Instant.EPOCH);
        experience.recordFix(4, 1L, null, "B", Instant.EPOCH);
        experience.recordFix(4, null, "Unknown service", "C", Instant.EPOCH); // no id: ignored for scoring
        var p = AssignmentFixtures.withFixes(sneha(0), Map.of(1L, 1, 8L, 2));
        var enriched = experience.enrich(List.of(p)).get(0);
        assertEquals(Map.of(1L, 3, 8L, 2), enriched.fixesByService());
    }
}

package in.ac.bitspilani.triagegraph.triage.domain;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.archived;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.atCosine;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex;

class KnnClassifierTest {

    @Test
    void similarResolvedIncidentsVoteWeightedBySimilarity() {
        var index = new InMemoryVectorIndex();
        index.put(archived(1, "HIST-1", "APPLICATION", "Fee Payment App", "r"), atCosine(0.9));
        index.put(archived(2, "HIST-2", "APPLICATION", "Fee Payment App", "r"), atCosine(0.8));
        index.put(archived(3, "HIST-3", "NETWORK", "Hostel Wi-Fi", "r"), atCosine(0.3));
        var c = new KnnClassifier(index, 5).classify(new TriageText("q", query())).orElseThrow();
        assertEquals("APPLICATION", c.categoryCode());
        assertEquals(1.7 / 2.0, c.confidence(), 1e-6);            // share 1.7/2.0, top similarity 0.9 → full trust
        assertEquals("Fee Payment App", c.serviceName());
        assertTrue(c.explanation().startsWith("2 of 3 similar resolved incidents were APPLICATION (closest HIST-1, 90% similar)"));
    }

    @Test
    void weakNeighboursReduceConfidence() {
        var index = new InMemoryVectorIndex();
        index.put(archived(1, "HIST-1", "STORAGE", "Cloud Storage", "r"), atCosine(0.225));
        var c = new KnnClassifier(index, 5).classify(new TriageText("q", query())).orElseThrow();
        assertEquals(0.5, c.confidence(), 1e-6);                   // share 1.0 × 0.225 / 0.45
    }

    @Test
    void noHistoryMeansNoOpinion() {
        assertTrue(new KnnClassifier(new InMemoryVectorIndex(), 5).classify(new TriageText("q", query())).isEmpty());
    }
}

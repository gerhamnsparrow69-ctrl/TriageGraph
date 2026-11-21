package in.ac.bitspilani.triagegraph.triage.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class EnsembleClassifierTest {

    private static final TriageText INPUT = new TriageText("x", new float[] {1f});

    private static IncidentClassifier fixed(String source, String category, double conf, String service, double serviceConf) {
        return new IncidentClassifier() {
            public Optional<ClassificationCandidate> classify(TriageText in) {
                return Optional.of(new ClassificationCandidate(category, conf, service == null ? null : 1L, service,
                        serviceConf, source, source + " says " + category));
            }

            public String source() {
                return source;
            }
        };
    }

    private static final IncidentClassifier BROKEN = new IncidentClassifier() {
        public Optional<ClassificationCandidate> classify(TriageText in) {
            throw new IllegalStateException("boom");
        }

        public String source() {
            return "LLM";
        }
    };

    private static EnsembleClassifier ensemble(IncidentClassifier... members) {
        return new EnsembleClassifier(List.of(members), 0.60, 0.10, "SOFTWARE");
    }

    @Test
    void moreTrustedSourceWinsWithinTheTieMargin() {
        var r = ensemble(fixed("KEYWORD", "NETWORK", 0.75, null, 0), fixed("KNN", "AUTHENTICATION", 0.70, null, 0)).classifyAll(INPUT);
        assertEquals("AUTHENTICATION", r.chosen().categoryCode()); // kNN is trusted more and only 0.05 behind
        assertEquals("KNN", r.chosen().source());
    }

    @Test
    void clearlyMoreConfidentVoteWinsEvenFromALessTrustedSource() {
        var r = ensemble(fixed("KEYWORD", "NETWORK", 0.90, null, 0), fixed("KNN", "AUTHENTICATION", 0.55, null, 0)).classifyAll(INPUT);
        assertEquals("NETWORK", r.chosen().categoryCode());
        assertTrue(r.lowConfidence() == false);
    }

    @Test
    void agreementAddsConfidenceAndIsExplained() {
        var r = ensemble(fixed("KEYWORD", "APPLICATION", 0.60, "Fee Payment App", 0.75),
                fixed("KNN", "APPLICATION", 0.80, "Fee Payment App", 0.70)).classifyAll(INPUT);
        assertEquals("APPLICATION", r.chosen().categoryCode());
        assertEquals(0.90, r.chosen().confidence(), 1e-9);
        assertTrue(r.chosen().explanation().endsWith("confirmed by keywords"), r.chosen().explanation());
        assertEquals("Fee Payment App", r.chosen().serviceName());
        assertEquals(2, r.votes().size());
    }

    @Test
    void aFailingMemberIsIgnored() {
        var r = ensemble(BROKEN, fixed("KNN", "NETWORK", 0.8, null, 0)).classifyAll(INPUT);
        assertEquals("NETWORK", r.chosen().categoryCode());
        assertEquals(1, r.votes().size());
    }

    @Test
    void noOpinionFallsBackToTheConfiguredCategoryWithZeroConfidence() {
        var r = ensemble(BROKEN, fixed("KEYWORD", null, 0, "Hostel Wi-Fi", 0.6)).classifyAll(INPUT);
        assertEquals("SOFTWARE", r.chosen().categoryCode());
        assertEquals("FALLBACK", r.chosen().source());
        assertEquals("Hostel Wi-Fi", r.chosen().serviceName());
        assertTrue(r.lowConfidence());
    }

    @Test
    void lowConfidenceIsFlagged() {
        var r = ensemble(fixed("KNN", "STORAGE", 0.4, null, 0)).classifyAll(INPUT);
        assertTrue(r.lowConfidence());
        assertFalse(ensemble(fixed("KNN", "STORAGE", 0.7, null, 0)).classifyAll(INPUT).lowConfidence());
    }
}

package in.ac.bitspilani.triagegraph.triage.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.triage.infrastructure.LexicalHashEmbedder;

/**
 * The labelled pairs load and the metrics are computed correctly. The MiniLM accuracy itself is checked in
 * {@code MiniLmDuplicateEvaluationTest}; here the lexical fallback shows that even without the model the
 * system stays precise (it just finds fewer paraphrases).
 */
class DuplicateEvaluationTest {

    @Test
    void thirtyLabelledPairsWithTwelveDuplicates() {
        var pairs = DuplicateEvaluation.loadDefault();
        assertEquals(30, pairs.size());
        assertEquals(12, pairs.stream().filter(DuplicateEvaluation.LabelledPair::duplicate).count());
    }

    @Test
    void metricsAreComputedFromTheConfusionMatrix() {
        var row = DuplicateEvaluation.row(java.util.List.of(new double[] {0.9, 1}, new double[] {0.7, 0},
                new double[] {0.3, 1}, new double[] {0.1, 0}), 0.5);
        assertEquals(1, row.tp());
        assertEquals(1, row.fp());
        assertEquals(1, row.fn());
        assertEquals(1, row.tn());
        assertEquals(0.5, row.precision(), 1e-9);
        assertEquals(0.5, row.f1(), 1e-9);
    }

    @Test
    void lexicalFallbackIsPreciseEvenThoughItMissesParaphrases() {
        var report = DuplicateEvaluation.evaluate(DuplicateEvaluation.loadDefault(), new LexicalHashEmbedder(), 0.62);
        assertEquals(30, report.results().size());
        assertTrue(report.best().f1() >= 0.6, "best F1 " + report.best());
        assertTrue(report.atThreshold().fp() <= 2, "few false alarms: " + report.atThreshold());
    }
}

package in.ac.bitspilani.triagegraph.triage.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.triage.application.DuplicateEvaluation;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicatePolicy;
import in.ac.bitspilani.triagegraph.triage.domain.TextNormalizer;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Runs the real in-process MiniLM model (no network needed). With the fp32 weights the calibration gave
 * F1 0.96 at the 0.62 threshold; LangChain4j ships the quantized model, so the bounds here leave a margin.
 */
class MiniLmDuplicateEvaluationTest {

    private final MiniLmTextEmbedder embedder = new MiniLmTextEmbedder();

    @Test
    void calibratedThresholdFindsDuplicatesWithTheRealModel() {
        var report = DuplicateEvaluation.evaluate(DuplicateEvaluation.loadDefault(), embedder, DuplicatePolicy.DEFAULT_LIKELY);
        System.out.println("MiniLM duplicate detection at " + DuplicatePolicy.DEFAULT_LIKELY + ": " + report.atThreshold()
                + " | best: " + report.best());
        assertTrue(report.atThreshold().f1() >= 0.80, "F1 " + report.atThreshold());
        assertTrue(report.atThreshold().recall() >= 0.80, "recall " + report.atThreshold());
    }

    @Test
    void workflowOneDemoReportIsALikelyDuplicateWithAMargin() {
        String first = TextNormalizer.combine("Fee payment app unavailable",
                "Students cannot complete fee transactions; the payment page fails to load since 10 am.");
        String second = TextNormalizer.combine("Fee payment not going through",
                "The payment page will not load so I cannot pay my semester fee.");
        double cosine = Vectors.cosine(embedder.embed(first), embedder.embed(second));
        double score = DuplicateDetector.combinedScore(cosine, true,
                DuplicateDetector.recency(Duration.ofMinutes(4), Duration.ofHours(72)));
        System.out.println("Workflow 1 pair: cosine " + Vectors.round3(cosine) + ", score " + Vectors.round3(score));
        assertTrue(score >= DuplicatePolicy.DEFAULT_LIKELY + 0.05, "score " + score);
    }
}

package in.ac.bitspilani.triagegraph.triage.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Measures duplicate detection on labelled pairs: the "Evaluate" step of Analyze → Model → Design →
 * Implement → Test → Evaluate → Defend. Each pair is scored exactly as production scores it (same weights),
 * assuming the two reports arrived 30 minutes apart.
 */
public final class DuplicateEvaluation {

    public static final String RESOURCE = "/triage/duplicate-pairs.tsv";
    static final Duration ASSUMED_GAP = Duration.ofMinutes(30);
    static final Duration WINDOW = Duration.ofHours(72);

    public record LabelledPair(String textA, String textB, boolean sameService, boolean duplicate, String kind) {
    }

    public record PairResult(String textA, String textB, boolean sameService, boolean duplicate, String kind,
            double cosine, double score, boolean predictedDuplicate) {
    }

    public record ThresholdRow(double threshold, int tp, int fp, int fn, int tn, double precision, double recall,
            double f1) {
    }

    public record Report(String embedder, int pairs, int positives, ThresholdRow atThreshold, ThresholdRow best,
            List<ThresholdRow> sweep, List<PairResult> results) {
    }

    private DuplicateEvaluation() {
    }

    public static List<LabelledPair> load(InputStream in) {
        if (in == null) {
            throw new IllegalStateException("Labelled pairs resource not found: " + RESOURCE);
        }
        List<LabelledPair> pairs = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = r.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                String[] f = line.split("\t", -1);
                if (f.length < 5) {
                    throw new IllegalStateException("Bad labelled pair: " + line);
                }
                pairs.add(new LabelledPair(f[0], f[1], Boolean.parseBoolean(f[2]), "1".equals(f[3].trim()), f[4]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(pairs);
    }

    public static List<LabelledPair> loadDefault() {
        return load(DuplicateEvaluation.class.getResourceAsStream(RESOURCE));
    }

    public static Report evaluate(List<LabelledPair> pairs, TextEmbedder embedder, double threshold) {
        double recency = DuplicateDetector.recency(ASSUMED_GAP, WINDOW);
        List<double[]> scored = new ArrayList<>();
        List<PairResult> results = new ArrayList<>();
        for (LabelledPair p : pairs) {
            double cosine = Vectors.cosine(embedder.embed(p.textA()), embedder.embed(p.textB()));
            double score = DuplicateDetector.combinedScore(cosine, p.sameService(), recency);
            scored.add(new double[] {score, p.duplicate() ? 1 : 0});
            results.add(new PairResult(p.textA(), p.textB(), p.sameService(), p.duplicate(), p.kind(),
                    Vectors.round3(cosine), Vectors.round3(score), score >= threshold));
        }
        List<ThresholdRow> sweep = new ArrayList<>();
        for (int t = 40; t <= 90; t += 2) {
            sweep.add(row(scored, t / 100.0));
        }
        ThresholdRow best = sweep.stream().max(Comparator.comparingDouble(ThresholdRow::f1)
                .thenComparingDouble(ThresholdRow::precision)).orElseThrow();
        int positives = (int) pairs.stream().filter(LabelledPair::duplicate).count();
        results.sort(Comparator.comparingDouble(PairResult::score).reversed());
        return new Report(embedder.name(), pairs.size(), positives, row(scored, threshold), best, sweep, results);
    }

    static ThresholdRow row(List<double[]> scored, double threshold) {
        int tp = 0, fp = 0, fn = 0, tn = 0;
        for (double[] s : scored) {
            boolean predicted = s[0] >= threshold;
            boolean actual = s[1] == 1;
            if (predicted && actual) {
                tp++;
            } else if (predicted) {
                fp++;
            } else if (actual) {
                fn++;
            } else {
                tn++;
            }
        }
        double precision = tp + fp == 0 ? 1.0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 1.0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        return new ThresholdRow(threshold, tp, fp, fn, tn, Vectors.round3(precision), Vectors.round3(recall), Vectors.round3(f1));
    }
}

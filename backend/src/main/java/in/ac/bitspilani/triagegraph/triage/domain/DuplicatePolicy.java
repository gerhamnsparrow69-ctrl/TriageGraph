package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Duration;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/**
 * Thresholds for the two bands. Defaults were calibrated on the 30 labelled pairs in
 * {@code triage/duplicate-pairs.tsv} with all-MiniLM-L6-v2: F1 peaked (0.96) for any likely threshold between
 * 0.60 and 0.64, so 0.62 sits in the middle of that plateau.
 */
public record DuplicatePolicy(double likelyThreshold, double possiblyThreshold, Duration window, int maxResults) {

    public static final double DEFAULT_LIKELY = 0.62;
    public static final double DEFAULT_POSSIBLY = 0.50;

    public DuplicatePolicy {
        if (!(possiblyThreshold > 0 && possiblyThreshold < likelyThreshold && likelyThreshold < 1)) {
            throw new InvalidInputException("Thresholds must satisfy 0 < possibly-related < likely-duplicate < 1.");
        }
        if (window == null || window.isNegative() || window.isZero() || window.compareTo(Duration.ofDays(30)) > 0) {
            throw new InvalidInputException("The duplicate window must be between 1 hour and 30 days.");
        }
        if (maxResults < 1) {
            throw new InvalidInputException("maxResults must be at least 1");
        }
    }
}

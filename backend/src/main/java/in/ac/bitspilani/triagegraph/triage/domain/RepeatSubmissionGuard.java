package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Duration;
import java.time.Instant;

import in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException;

/**
 * Stops accidental double submissions: the same reporter sending near-identical text again within a short
 * window gets a 409 that points to the incident they already have.
 */
public class RepeatSubmissionGuard {

    private final VectorIndex index;

    public RepeatSubmissionGuard(VectorIndex index) {
        this.index = index;
    }

    /** @throws DuplicateIncidentException near-identical text from the same reporter inside the window */
    public void check(long reporterId, float[] vector, Instant now, Duration window, double threshold) {
        Instant since = now.minus(window);
        index.nearest(vector, 1, v -> v.source() == KnowledgeSource.LIVE && v.reporterId() != null
                        && v.reporterId() == reporterId && v.createdAt() != null && !v.createdAt().isBefore(since))
                .stream()
                .filter(s -> s.similarity() >= threshold)
                .findFirst()
                .ifPresent(s -> {
                    long minutes = Math.max(0, Duration.between(s.view().createdAt(), now).toMinutes());
                    throw new DuplicateIncidentException("You reported this " + (minutes == 0 ? "just now" : minutes + " min ago")
                            + " as " + s.view().code() + ". Please add details to that incident instead.");
                });
    }
}

package in.ac.bitspilani.triagegraph.sla.domain;

import java.time.Instant;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

public record SlaEvaluationInput(
        IncidentStatus status,
        Instant respondedAt,
        Instant responseWarningAt,
        Instant responseDueAt,
        boolean responseWarningRecorded,
        boolean responseBreachRecorded,
        Instant resolutionWarningAt,
        Instant resolutionDueAt,
        boolean resolutionWarningRecorded,
        boolean resolutionBreachRecorded,
        boolean resolutionPaused,
        Instant now) {

    public SlaEvaluationInput {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(now, "now");
        validateThresholdOrder(responseWarningAt, responseDueAt,
                "responseWarningAt must not be later than responseDueAt");
        validateThresholdOrder(resolutionWarningAt, resolutionDueAt,
                "resolutionWarningAt must not be later than resolutionDueAt");
    }

    private static void validateThresholdOrder(Instant warningAt, Instant dueAt, String message) {
        if (warningAt != null && dueAt != null && warningAt.isAfter(dueAt)) {
            throw new IllegalArgumentException(message);
        }
    }
}

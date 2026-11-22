package in.ac.bitspilani.triagegraph.sla.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

public final class SlaEvaluator {

    public enum Decision {
        RESPONSE_WARNING,
        RESOLUTION_WARNING,
        RESPONSE_BREACH,
        RESOLUTION_BREACH
    }

    public List<Decision> evaluate(SlaEvaluationInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (isTerminal(input.status())) {
            return List.of();
        }

        boolean responseActive = input.respondedAt() == null
                && input.responseDueAt() != null
                && !input.responseBreachRecorded();
        boolean resolutionActive = input.resolutionDueAt() != null
                && !input.resolutionPaused()
                && !input.resolutionBreachRecorded()
                && !isResolutionInactive(input.status());

        List<Decision> decisions = new ArrayList<>(4);
        if (responseActive && !input.responseWarningRecorded()
                && reached(input.responseWarningAt(), input.now())) {
            decisions.add(Decision.RESPONSE_WARNING);
        }
        if (resolutionActive && !input.resolutionWarningRecorded()
                && reached(input.resolutionWarningAt(), input.now())) {
            decisions.add(Decision.RESOLUTION_WARNING);
        }
        if (responseActive && !input.responseBreachRecorded()
                && reached(input.responseDueAt(), input.now())) {
            decisions.add(Decision.RESPONSE_BREACH);
        }
        if (resolutionActive && !input.resolutionBreachRecorded()
                && reached(input.resolutionDueAt(), input.now())) {
            decisions.add(Decision.RESOLUTION_BREACH);
        }
        return List.copyOf(decisions);
    }

    private static boolean reached(Instant threshold, Instant now) {
        return threshold != null && !now.isBefore(threshold);
    }

    private static boolean isTerminal(IncidentStatus status) {
        return status == IncidentStatus.CLOSED || status == IncidentStatus.MERGED;
    }

    private static boolean isResolutionInactive(IncidentStatus status) {
        return status == IncidentStatus.PENDING_INFO
                || status == IncidentStatus.RESOLVED
                || status == IncidentStatus.VERIFIED;
    }
}

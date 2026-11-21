package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.exception.FeedbackNotAllowedException;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Terminal. Only the reporter's one-time feedback is allowed. */
final class ClosedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.noneOf(IncidentStatus.class));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.FEEDBACK));

    @Override
    public IncidentStatus status() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public Set<IncidentStatus> allowedNext() {
        return NEXT;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ACTIONS;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public void submitFeedback(Incident i, int rating, String comment, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.FEEDBACK, by);
        if (i.getFeedback() != null) {
            throw new FeedbackNotAllowedException("Feedback for " + i.label() + " was already submitted.");
        }
        i.recordFeedback(rating, comment, by, now);
    }
}

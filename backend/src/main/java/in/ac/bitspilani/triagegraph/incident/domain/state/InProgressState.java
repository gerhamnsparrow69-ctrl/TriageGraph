package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;

/** The assignee is working on it. */
final class InProgressState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(PENDING_INFO, RESOLVED));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.REQUEST_INFO, IncidentAction.RESOLVE));

    @Override
    public IncidentStatus status() {
        return IN_PROGRESS;
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
    public void requestInfo(Incident i, String question, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.REQUEST_INFO, by);
        i.appendNote(question, false, by, now);
        i.sla().pause(now);
        i.transitionTo(PENDING_INFO, by, now, "Waiting for reporter: " + question.strip());
    }

    @Override
    public void resolve(Incident i, String summary, String rootCause, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.RESOLVE, by);
        i.recordResolution(summary, rootCause, by, now);
        i.sla().pause(now);
        i.transitionTo(RESOLVED, by, now, "Resolved: " + i.getResolution().getSummary());
    }
}

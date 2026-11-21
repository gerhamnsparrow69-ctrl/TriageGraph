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

/** Waiting for the reporter to answer a question; the SLA clock is paused. */
final class PendingInfoState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.PROVIDE_INFO));

    @Override
    public IncidentStatus status() {
        return PENDING_INFO;
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
    public void provideInfo(Incident i, String answer, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.PROVIDE_INFO, by);
        i.appendNote(answer, false, by, now);
        i.sla().resume(now);
        i.transitionTo(IN_PROGRESS, by, now, "Reporter replied");
    }
}

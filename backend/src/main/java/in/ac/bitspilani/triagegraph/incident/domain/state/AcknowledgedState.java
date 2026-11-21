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

/** The assignee has accepted the incident; the response SLA is met or breached. */
final class AcknowledgedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, ASSIGNED));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.START_WORK, IncidentAction.ASSIGN));

    @Override
    public IncidentStatus status() {
        return ACKNOWLEDGED;
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
    public void startWork(Incident i, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.START_WORK, by);
        i.transitionTo(IN_PROGRESS, by, now, "Work started");
    }

    @Override
    public void assign(Incident i, long technicianId, String strategyKey, Actor by, Instant now) {
        doReassign(i, technicianId, strategyKey, by, now);
    }
}

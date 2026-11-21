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

/** A technician has it but has not acknowledged yet (the response SLA is running). */
final class AssignedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(ACKNOWLEDGED, ASSIGNED));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.ACKNOWLEDGE, IncidentAction.ASSIGN));

    @Override
    public IncidentStatus status() {
        return ASSIGNED;
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
    public void acknowledge(Incident i, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.ACKNOWLEDGE, by);
        i.sla().markResponded(now);
        i.transitionTo(ACKNOWLEDGED, by, now, "Technician acknowledged");
    }

    @Override
    public void assign(Incident i, long technicianId, String strategyKey, Actor by, Instant now) {
        doReassign(i, technicianId, strategyKey, by, now);
    }
}

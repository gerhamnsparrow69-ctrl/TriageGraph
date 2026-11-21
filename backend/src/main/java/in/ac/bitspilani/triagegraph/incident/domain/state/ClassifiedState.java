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

/** Category, service, priority and SLA known; waiting for a technician. */
final class ClassifiedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(ASSIGNED, MERGED));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.ASSIGN, IncidentAction.MERGE));

    @Override
    public IncidentStatus status() {
        return CLASSIFIED;
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
    public void assign(Incident i, long technicianId, String strategyKey, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.ASSIGN, by);
        i.changeAssignee(technicianId, strategyKey, by, now);
    }

    @Override
    public void mergeInto(Incident i, Incident parent, Actor by, Instant now) {
        doMerge(i, parent, by, now);
    }
}

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

/** Fixed; waiting for the reporter to verify or reopen. The SLA clock is paused. */
final class ResolvedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(VERIFIED, IN_PROGRESS));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.VERIFY, IncidentAction.REOPEN));

    @Override
    public IncidentStatus status() {
        return RESOLVED;
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
    public void verify(Incident i, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.VERIFY, by);
        i.transitionTo(VERIFIED, by, now, by.isSystem() ? "Verified automatically" : "Reporter confirmed the fix");
    }

    @Override
    public void reopen(Incident i, String reason, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.REOPEN, by);
        String why = Incident.requireText(reason, "Reopen reason", 1000);
        i.discardResolution();
        i.sla().resume(now);
        i.transitionTo(IN_PROGRESS, by, now, "Reopened: " + why);
    }
}

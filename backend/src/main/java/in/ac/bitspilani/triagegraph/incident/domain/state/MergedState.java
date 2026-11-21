package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Terminal. Folded into another incident as a duplicate; nothing more can happen here. */
final class MergedState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.noneOf(IncidentStatus.class));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.noneOf(IncidentAction.class));

    @Override
    public IncidentStatus status() {
        return IncidentStatus.MERGED;
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
}

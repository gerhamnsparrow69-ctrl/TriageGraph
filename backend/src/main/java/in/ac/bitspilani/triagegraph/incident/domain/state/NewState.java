package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.ClassificationInfo;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;

/** Just reported; waiting for a category and service. */
final class NewState extends IncidentState {

    private static final Set<IncidentStatus> NEXT =
            Collections.unmodifiableSet(EnumSet.of(CLASSIFIED, MERGED));
    private static final Set<IncidentAction> ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(IncidentAction.CLASSIFY, IncidentAction.MERGE));

    @Override
    public IncidentStatus status() {
        return NEW;
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
    public void classify(Incident i, String categoryCode, long serviceId, ClassificationInfo info,
            Actor by, Instant now) {
        i.requirePermitted(IncidentAction.CLASSIFY, by);
        i.recordClassification(categoryCode, serviceId, info);
        i.transitionTo(CLASSIFIED, by, now, "Classified as " + categoryCode
                + (info == null || info.getSource() == null ? "" : " (" + info.getSource() + ")"));
    }

    @Override
    public void mergeInto(Incident i, Incident parent, Actor by, Instant now) {
        doMerge(i, parent, by, now);
    }
}

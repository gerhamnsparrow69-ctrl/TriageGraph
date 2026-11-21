package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.ClassificationInfo;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One subclass per {@link IncidentStatus} (State pattern). Every action is rejected here by default;
 * a state overrides only the actions its row of the transition table allows (blueprint §6):
 * <pre>
 * NEW          classify → CLASSIFIED; mergeInto → MERGED
 * CLASSIFIED   assign → ASSIGNED; mergeInto → MERGED
 * ASSIGNED     acknowledge → ACKNOWLEDGED; reassign → ASSIGNED
 * ACKNOWLEDGED startWork → IN_PROGRESS; reassign → ASSIGNED
 * IN_PROGRESS  requestInfo → PENDING_INFO; resolve → RESOLVED
 * PENDING_INFO provideInfo → IN_PROGRESS (SLA clock resumes)
 * RESOLVED     verify → VERIFIED; reopen → IN_PROGRESS
 * VERIFIED     close → CLOSED
 * CLOSED       feedback only;   MERGED: nothing
 * </pre>
 * Order inside an allowed action: state check (409) first, then who may do it (403), then input (400).
 * States hold no data, so each is a stateless singleton in {@link IncidentStateRegistry}.
 */
public abstract class IncidentState {

    public abstract IncidentStatus status();

    /** Statuses reachable from this one; also used by the incident to guard every move. */
    public abstract Set<IncidentStatus> allowedNext();

    /** Actions this state accepts, before checking who is asking. */
    public abstract Set<IncidentAction> allowedActions();

    public boolean isTerminal() {
        return false;
    }

    public void classify(Incident i, String categoryCode, long serviceId, ClassificationInfo info,
            Actor by, Instant now) {
        reject(IncidentAction.CLASSIFY);
    }

    public void assign(Incident i, long technicianId, String strategyKey, Actor by, Instant now) {
        reject(IncidentAction.ASSIGN);
    }

    public void acknowledge(Incident i, Actor by, Instant now) {
        reject(IncidentAction.ACKNOWLEDGE);
    }

    public void startWork(Incident i, Actor by, Instant now) {
        reject(IncidentAction.START_WORK);
    }

    public void requestInfo(Incident i, String question, Actor by, Instant now) {
        reject(IncidentAction.REQUEST_INFO);
    }

    public void provideInfo(Incident i, String answer, Actor by, Instant now) {
        reject(IncidentAction.PROVIDE_INFO);
    }

    public void resolve(Incident i, String summary, String rootCause, Actor by, Instant now) {
        reject(IncidentAction.RESOLVE);
    }

    public void verify(Incident i, Actor by, Instant now) {
        reject(IncidentAction.VERIFY);
    }

    public void reopen(Incident i, String reason, Actor by, Instant now) {
        reject(IncidentAction.REOPEN);
    }

    public void close(Incident i, Actor by, Instant now) {
        reject(IncidentAction.CLOSE);
    }

    public void mergeInto(Incident i, Incident parent, Actor by, Instant now) {
        reject(IncidentAction.MERGE);
    }

    public void submitFeedback(Incident i, int rating, String comment, Actor by, Instant now) {
        reject(IncidentAction.FEEDBACK);
    }

    /** Shared by NEW and CLASSIFIED. */
    protected final void doMerge(Incident i, Incident parent, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.MERGE, by);
        i.linkAsDuplicateOf(parent, by, now);
        i.transitionTo(IncidentStatus.MERGED, by, now, "Merged into " + parent.label());
    }

    /** Shared by ASSIGNED and ACKNOWLEDGED. */
    protected final void doReassign(Incident i, long technicianId, String strategyKey, Actor by, Instant now) {
        i.requirePermitted(IncidentAction.ASSIGN, by);
        i.changeAssignee(technicianId, strategyKey, by, now);
    }

    protected final void reject(IncidentAction action) {
        String allowed = allowedActions().isEmpty() ? "none"
                : allowedActions().stream().map(IncidentState::verb).collect(Collectors.joining(", "));
        throw new InvalidStateTransitionException("Cannot " + verb(action) + " an incident that is "
                + status() + ". Allowed now: " + allowed + ".");
    }

    private static String verb(IncidentAction a) {
        return a.name().toLowerCase().replace('_', ' ');
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

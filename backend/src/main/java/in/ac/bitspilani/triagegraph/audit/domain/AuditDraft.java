package in.ac.bitspilani.triagegraph.audit.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

import java.time.Instant;

/** An audit entry before it is chained: everything except prevHash and hash. */
public record AuditDraft(Instant occurredAt, Long actorId, Role actorRole, AuditAction action,
        Long incidentId, String incidentCode, IncidentStatus fromState, IncidentStatus toState,
        String description) {

    public AuditDraft withActorRole(Role role) {
        return new AuditDraft(occurredAt, actorId, role, action, incidentId, incidentCode, fromState, toState,
                description);
    }

    public AuditDraft withFromState(IncidentStatus from) {
        return new AuditDraft(occurredAt, actorId, actorRole, action, incidentId, incidentCode, from, toState,
                description);
    }
}

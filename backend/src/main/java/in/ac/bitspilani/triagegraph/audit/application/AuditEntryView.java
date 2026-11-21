package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

import java.time.Instant;

public record AuditEntryView(long id, Instant occurredAt, Long actorId, Role actorRole, AuditAction action,
        Long incidentId, String incidentCode, IncidentStatus fromState, IncidentStatus toState,
        String description, String prevHash, String hash) {

    static AuditEntryView of(AuditEntry e) {
        return new AuditEntryView(e.getId(), e.getOccurredAt(), e.getActorId(), e.getActorRole(), e.getAction(),
                e.getIncidentId(), e.getIncidentCode(), e.getFromState(), e.getToState(), e.getDescription(),
                e.getPrevHash(), e.getHash());
    }
}

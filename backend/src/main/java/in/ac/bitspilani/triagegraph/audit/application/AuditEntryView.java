package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

import java.time.Instant;
import java.util.function.Function;

/** {@code actorName} is display-only (not part of the hash); null for system entries or an unknown user. */
public record AuditEntryView(long id, Instant occurredAt, Long actorId, Role actorRole, String actorName,
        AuditAction action,
        Long incidentId, String incidentCode, IncidentStatus fromState, IncidentStatus toState,
        String description, String prevHash, String hash) {

    static AuditEntryView of(AuditEntry e, Function<Long, String> names) {
        return new AuditEntryView(e.getId(), e.getOccurredAt(), e.getActorId(), e.getActorRole(),
                names.apply(e.getActorId()), e.getAction(),
                e.getIncidentId(), e.getIncidentCode(), e.getFromState(), e.getToState(), e.getDescription(),
                e.getPrevHash(), e.getHash());
    }
}

package in.ac.bitspilani.triagegraph.shared.contracts.events;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import java.time.Instant;

public record SlaWarningRaised(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        SlaKind kind, Instant dueAt) implements IncidentEvent {
}

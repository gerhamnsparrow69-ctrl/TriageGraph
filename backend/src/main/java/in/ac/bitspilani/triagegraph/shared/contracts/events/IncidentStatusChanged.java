package in.ac.bitspilani.triagegraph.shared.contracts.events;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import java.time.Instant;

public record IncidentStatusChanged(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        IncidentStatus from, IncidentStatus to, String reason) implements IncidentEvent {
}

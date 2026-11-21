package in.ac.bitspilani.triagegraph.shared.contracts.events;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import java.time.Instant;

public record PriorityAssigned(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        Priority previous, Priority current, double score, String rationale) implements IncidentEvent {
}

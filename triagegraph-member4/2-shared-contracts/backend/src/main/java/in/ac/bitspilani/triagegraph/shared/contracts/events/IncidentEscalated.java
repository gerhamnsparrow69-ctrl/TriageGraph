package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record IncidentEscalated(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        int level, long targetUserId, String reason) implements IncidentEvent {
}

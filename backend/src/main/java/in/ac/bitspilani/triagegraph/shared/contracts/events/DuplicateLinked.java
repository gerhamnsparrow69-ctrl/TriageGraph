package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record DuplicateLinked(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        long duplicateOfId, long userId, boolean merged) implements IncidentEvent {
}

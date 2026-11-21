package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record IncidentReported(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        long serviceId, String categoryCode) implements IncidentEvent {
}

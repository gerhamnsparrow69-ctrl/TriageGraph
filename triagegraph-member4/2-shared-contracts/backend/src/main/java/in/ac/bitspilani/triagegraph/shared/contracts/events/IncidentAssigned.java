package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record IncidentAssigned(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        Long previousTechnicianId, long technicianId, String strategyKey) implements IncidentEvent {
}

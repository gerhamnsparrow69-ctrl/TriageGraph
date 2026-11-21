package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record FeedbackSubmitted(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        int rating, String comment) implements IncidentEvent {
}

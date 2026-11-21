package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

public record NoteAdded(long incidentId, String incidentCode, Long actorId, Instant occurredAt,
        long noteId, boolean internal) implements IncidentEvent {
}

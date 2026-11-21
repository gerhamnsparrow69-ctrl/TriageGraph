package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;
import java.util.List;

public record MajorIncidentDeclared(long majorIncidentId, String code, Long actorId, Instant occurredAt,
        long rootCauseServiceId, List<Long> childIds) implements DomainEvent {
}

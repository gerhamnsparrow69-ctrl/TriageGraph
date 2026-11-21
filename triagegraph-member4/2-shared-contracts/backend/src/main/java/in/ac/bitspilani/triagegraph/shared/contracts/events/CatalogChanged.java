package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

/** A service, dependency or category was added, edited or removed. {@code entityType}: SERVICE, DEPENDENCY or CATEGORY. */
public record CatalogChanged(Long actorId, Instant occurredAt,
        String entityType, String entityKey, String change) implements DomainEvent {
}

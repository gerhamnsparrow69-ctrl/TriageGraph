package in.ac.bitspilani.triagegraph.shared.contracts.events;

import java.time.Instant;

/**
 * Everything that happens in the system. Sealed, so listeners can use an exhaustive switch.
 * {@code actorId} is null for system actions (SLA monitor, correlation).
 */
public sealed interface DomainEvent permits IncidentEvent, MajorIncidentDeclared, CatalogChanged {

    Long actorId();

    Instant occurredAt();
}

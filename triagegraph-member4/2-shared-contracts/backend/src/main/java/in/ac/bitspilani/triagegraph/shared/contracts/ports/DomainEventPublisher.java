package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import java.util.List;

/** Provided by Member 4 (config). */
public interface DomainEventPublisher {

    void publishAll(List<? extends DomainEvent> events);
}

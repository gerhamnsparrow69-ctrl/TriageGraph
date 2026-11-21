package in.ac.bitspilani.triagegraph.config;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;

/**
 * Hands domain events to Spring's event bus. Listeners choose their phase with
 * {@code @TransactionalEventListener} (audit: BEFORE_COMMIT; notifications: AFTER_COMMIT + {@code @Async}).
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAll(List<? extends DomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }
}

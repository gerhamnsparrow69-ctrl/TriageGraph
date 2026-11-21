package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import in.ac.bitspilani.triagegraph.notification.domain.DeliveryStatus;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationChannel;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLog;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLogRepository;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationTrigger;
import in.ac.bitspilani.triagegraph.notification.domain.Recipient;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.exception.NotificationDeliveryException;

/**
 * Observer: reacts to domain events after the transaction commits, on a background thread, so a
 * slow or broken mail server never delays or rolls back the incident itself.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final RecipientResolver resolver;
    private final ChannelSelectionPolicy policy;
    private final NotificationMessageFactory messages;
    private final NotificationChannelFactory channels;
    private final NotificationLogRepository logs;
    private final Clock clock;

    public NotificationDispatcher(RecipientResolver resolver, ChannelSelectionPolicy policy,
            NotificationMessageFactory messages, NotificationChannelFactory channels,
            NotificationLogRepository logs, Clock clock) {
        this.resolver = resolver;
        this.policy = policy;
        this.messages = messages;
        this.channels = channels;
        this.logs = logs;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void on(DomainEvent event) {
        try {
            resolver.resolve(event).ifPresent(this::dispatch);
        } catch (RuntimeException e) {
            log.error("Could not dispatch notifications for {}", event, e);
        }
    }

    public void dispatch(NotificationTrigger trigger) {
        for (Recipient r : trigger.recipients()) {
            NotificationMessage message = messages.create(trigger, r);
            for (ChannelType type : policy.channelsFor(trigger.eventType(), r.role(), trigger.priority())) {
                deliver(type, r, message);
            }
        }
    }

    private void deliver(ChannelType type, Recipient r, NotificationMessage message) {
        DeliveryStatus status;
        String error = null;
        if (!channels.has(type)) {
            status = DeliveryStatus.SKIPPED;
            error = type + " disabled";
        } else {
            NotificationChannel channel = channels.get(type);
            if (!channel.canReach(r.user())) {
                status = DeliveryStatus.SKIPPED;
                error = "no " + type + " address";
            } else {
                try {
                    channel.send(r.user(), message);
                    status = DeliveryStatus.SENT;
                } catch (NotificationDeliveryException e) {
                    status = DeliveryStatus.FAILED;
                    error = e.getMessage();
                    log.warn("{} to user {} failed after retries: {}", type, r.user().id(), e.getMessage());
                }
            }
        }
        logs.save(new NotificationLog(r.user().id(), type, message.eventType(), message.incidentId(), status, error,
                Instant.now(clock)));
    }
}

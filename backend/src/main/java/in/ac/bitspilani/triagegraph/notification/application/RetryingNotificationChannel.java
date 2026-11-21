package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationChannel;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.exception.NotificationDeliveryException;

/** Decorator: retries a channel with doubling backoff, then rethrows the last failure. */
public class RetryingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(RetryingNotificationChannel.class);

    private final NotificationChannel delegate;
    private final int attempts;
    private final Duration backoff;
    private final Sleeper sleeper;

    public RetryingNotificationChannel(NotificationChannel delegate, int attempts, Duration backoff, Sleeper sleeper) {
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be at least 1");
        }
        this.delegate = delegate;
        this.attempts = attempts;
        this.backoff = backoff;
        this.sleeper = sleeper;
    }

    @Override
    public ChannelType type() {
        return delegate.type();
    }

    @Override
    public boolean canReach(UserRef recipient) {
        return delegate.canReach(recipient);
    }

    @Override
    public void send(UserRef recipient, NotificationMessage message) {
        Duration wait = backoff;
        for (int attempt = 1; ; attempt++) {
            try {
                delegate.send(recipient, message);
                return;
            } catch (NotificationDeliveryException e) {
                if (attempt >= attempts) {
                    throw e;
                }
                log.warn("{} attempt {}/{} failed for user {}: {}", type(), attempt, attempts, recipient.id(), e.getMessage());
                sleeper.sleep(wait);
                wait = wait.multipliedBy(2);
            }
        }
    }
}

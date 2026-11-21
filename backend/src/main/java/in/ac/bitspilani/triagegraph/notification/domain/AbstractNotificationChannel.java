package in.ac.bitspilani.triagegraph.notification.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.exception.NotificationDeliveryException;

/**
 * Template method: {@link #send} is fixed, subclasses only implement {@link #deliver}.
 * Any failure comes out as a {@link NotificationDeliveryException}, so callers handle one type.
 */
public abstract class AbstractNotificationChannel implements NotificationChannel {

    @Override
    public boolean canReach(UserRef recipient) {
        return true;
    }

    @Override
    public final void send(UserRef recipient, NotificationMessage message) {
        if (!canReach(recipient)) {
            throw new NotificationDeliveryException(type() + " cannot reach user " + recipient.id());
        }
        try {
            deliver(recipient, message);
        } catch (NotificationDeliveryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new NotificationDeliveryException(type() + " delivery failed: " + e.getMessage(), e);
        }
    }

    protected abstract void deliver(UserRef recipient, NotificationMessage message);
}

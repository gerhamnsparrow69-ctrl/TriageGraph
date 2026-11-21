package in.ac.bitspilani.triagegraph.notification.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;

/**
 * One way of reaching a user. Adding a channel (Telegram, SMS...) means one new class
 * plus rule rows in {@code NotificationRules}; nothing else changes.
 */
public interface NotificationChannel {

    ChannelType type();

    /** False when the user has nothing this channel can deliver to, e.g. no push topic. */
    boolean canReach(UserRef recipient);

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.NotificationDeliveryException on failure */
    void send(UserRef recipient, NotificationMessage message);
}

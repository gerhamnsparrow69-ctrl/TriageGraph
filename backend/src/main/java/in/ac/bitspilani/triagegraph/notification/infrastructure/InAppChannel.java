package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.notification.application.LivePush;
import in.ac.bitspilani.triagegraph.notification.domain.AbstractNotificationChannel;
import in.ac.bitspilani.triagegraph.notification.domain.Notification;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;

/** Stores the notification in the user's inbox and pushes it to any open browser tab. */
@Component
class InAppChannel extends AbstractNotificationChannel {

    static final String EVENT = "notification";

    private final NotificationRepository inbox;
    private final LivePush push;
    private final Clock clock;

    InAppChannel(NotificationRepository inbox, LivePush push, Clock clock) {
        this.inbox = inbox;
        this.push = push;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.IN_APP;
    }

    @Override
    protected void deliver(UserRef recipient, NotificationMessage message) {
        Notification saved = inbox.save(new Notification(message, Instant.now(clock)));
        push.toUser(recipient.id(), EVENT, NotificationView.from(saved));
    }
}

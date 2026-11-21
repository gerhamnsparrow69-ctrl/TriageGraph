package in.ac.bitspilani.triagegraph.notification.infrastructure;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.notification.domain.AbstractNotificationChannel;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** Phone push via ntfy to the user's private topic. P1 uses max priority so the phone buzzes. */
@Component
class PushChannel extends AbstractNotificationChannel {

    private final NtfyGateway ntfy;

    PushChannel(NtfyGateway ntfy) {
        this.ntfy = ntfy;
    }

    @Override
    public ChannelType type() {
        return ChannelType.PUSH;
    }

    @Override
    public boolean canReach(UserRef recipient) {
        return recipient.pushTopic() != null && !recipient.pushTopic().isBlank();
    }

    @Override
    protected void deliver(UserRef recipient, NotificationMessage message) {
        ntfy.publish(recipient.pushTopic(), message.subject(), message.body(), ntfyPriority(message.priority()));
    }

    static int ntfyPriority(Priority p) {
        if (p == null) {
            return 3;
        }
        return switch (p) {
            case P1 -> 5;
            case P2 -> 4;
            case P3 -> 3;
            case P4 -> 2;
        };
    }
}

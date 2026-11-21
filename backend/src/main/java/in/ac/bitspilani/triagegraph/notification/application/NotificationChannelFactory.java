package in.ac.bitspilani.triagegraph.notification.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationChannel;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.exception.StrategyNotFoundException;

/** Hands out each channel already wrapped in the retry decorator. Disabled channels are left out. */
public class NotificationChannelFactory {

    private final Map<ChannelType, NotificationChannel> channels = new EnumMap<>(ChannelType.class);

    public NotificationChannelFactory(List<NotificationChannel> available, NotifyProperties props, Sleeper sleeper) {
        for (NotificationChannel c : available) {
            if (c.type() == ChannelType.EMAIL && !props.emailEnabled()
                    || c.type() == ChannelType.PUSH && !props.pushEnabled()) {
                continue;
            }
            channels.put(c.type(), new RetryingNotificationChannel(c, props.retryAttempts(), props.retryBackoff(), sleeper));
        }
    }

    public NotificationChannel get(ChannelType type) {
        NotificationChannel c = channels.get(type);
        if (c == null) {
            throw new StrategyNotFoundException("No enabled notification channel for " + type);
        }
        return c;
    }

    public boolean has(ChannelType type) {
        return channels.containsKey(type);
    }

    public Set<ChannelType> enabled() {
        return Set.copyOf(channels.keySet());
    }
}

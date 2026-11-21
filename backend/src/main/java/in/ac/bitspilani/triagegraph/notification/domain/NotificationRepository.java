package in.ac.bitspilani.triagegraph.notification.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification n);

    Optional<Notification> findById(long id);

    List<Notification> latestFor(long recipientId, int limit);

    long unreadCount(long recipientId);

    List<Notification> unreadFor(long recipientId);
}

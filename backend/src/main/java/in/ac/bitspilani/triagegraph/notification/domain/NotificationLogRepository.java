package in.ac.bitspilani.triagegraph.notification.domain;

import java.util.List;

/** Append-only: no update or delete. */
public interface NotificationLogRepository {

    NotificationLog save(NotificationLog entry);

    List<NotificationLog> latest(int limit);
}

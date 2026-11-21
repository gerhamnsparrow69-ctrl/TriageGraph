package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.repository.Repository;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationLog;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLogRepository;

/** Extends the bare Repository marker so no update or delete method is exposed. */
interface JpaNotificationLogRepository extends Repository<NotificationLog, Long>, NotificationLogRepository {

    @Override
    NotificationLog save(NotificationLog entry);

    List<NotificationLog> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Override
    default List<NotificationLog> latest(int limit) {
        return findAllByOrderByCreatedAtDescIdDesc(Limit.of(limit));
    }
}

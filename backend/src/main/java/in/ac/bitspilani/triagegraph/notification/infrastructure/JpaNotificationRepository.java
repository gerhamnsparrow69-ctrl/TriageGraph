package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.notification.domain.Notification;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationRepository;

@Repository
class JpaNotificationRepository implements NotificationRepository {

    private final SpringDataNotificationRepository jpa;

    JpaNotificationRepository(SpringDataNotificationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Notification save(Notification n) {
        return jpa.save(n);
    }

    @Override
    public Optional<Notification> findById(long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Notification> latestFor(long recipientId, int limit) {
        return jpa.findByRecipientIdOrderByCreatedAtDescIdDesc(recipientId, Limit.of(limit));
    }

    @Override
    public long unreadCount(long recipientId) {
        return jpa.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Override
    public List<Notification> unreadFor(long recipientId) {
        return jpa.findByRecipientIdAndReadAtIsNull(recipientId);
    }
}

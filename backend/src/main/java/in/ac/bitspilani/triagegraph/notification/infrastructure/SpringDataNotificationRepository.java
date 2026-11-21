package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.notification.domain.Notification;

interface SpringDataNotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(long recipientId, Limit limit);

    long countByRecipientIdAndReadAtIsNull(long recipientId);

    List<Notification> findByRecipientIdAndReadAtIsNull(long recipientId);
}

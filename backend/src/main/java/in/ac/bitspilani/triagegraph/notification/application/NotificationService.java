package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.notification.domain.Notification;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLog;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLogRepository;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationRepository;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationTrigger;
import in.ac.bitspilani.triagegraph.notification.domain.Recipient;
import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import in.ac.bitspilani.triagegraph.shared.exception.ResourceNotFoundException;

/** The inbox behind the bell: list, count, mark read. */
@Service
public class NotificationService {

    public static final int PAGE = 50;

    private final NotificationRepository inbox;
    private final NotificationLogRepository logs;
    private final NotificationDispatcher dispatcher;
    private final UserDirectory users;
    private final LiveFeed feed;
    private final Clock clock;

    public NotificationService(NotificationRepository inbox, NotificationLogRepository logs,
            NotificationDispatcher dispatcher, UserDirectory users, LiveFeed feed, Clock clock) {
        this.inbox = inbox;
        this.logs = logs;
        this.dispatcher = dispatcher;
        this.users = users;
        this.feed = feed;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Notification> latest(long userId) {
        return inbox.latestFor(userId, PAGE);
    }

    @Transactional(readOnly = true)
    public long unread(long userId) {
        return inbox.unreadCount(userId);
    }

    @Transactional
    public Notification markRead(long userId, long notificationId) {
        Notification n = inbox.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFound(notificationId));
        if (!n.belongsTo(userId)) {
            throw new UnauthorizedOperationException("That notification belongs to someone else.");
        }
        n.markRead(Instant.now(clock));
        return n;
    }

    @Transactional
    public int markAllRead(long userId) {
        Instant now = Instant.now(clock);
        List<Notification> unread = inbox.unreadFor(userId);
        unread.forEach(n -> n.markRead(now));
        return unread.size();
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> deliveryLog() {
        return logs.latest(100);
    }

    /** Admin check that every channel reaches a user: sends a TEST notification on all of them. */
    public void sendTest(long userId) {
        var user = users.user(userId);
        dispatcher.dispatch(new NotificationTrigger(NotificationEvents.TEST, null, null, null, null, null,
                List.of(new Recipient(user, RecipientRole.ADMIN))));
        feed.publish(new FeedItem(NotificationEvents.TEST, null, "Test notification sent to " + user.name(),
                Instant.now(clock)));
    }

    static class NotificationNotFound extends ResourceNotFoundException {
        NotificationNotFound(long id) {
            super("NOTIFICATION_NOT_FOUND", "No notification with id " + id);
        }
    }
}

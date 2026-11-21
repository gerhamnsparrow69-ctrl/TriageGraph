package in.ac.bitspilani.triagegraph.notification.domain;

import java.time.Instant;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** A row in a user's in-app inbox (the bell). */
@Entity
@Table(name = "notifications", indexes = @Index(name = "ix_notifications_recipient", columnList = "recipientId, createdAt"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long recipientId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private Priority priority;

    @Column(nullable = false, length = 40)
    private String eventType;

    private Long incidentId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    protected Notification() {
    }

    public Notification(NotificationMessage m, Instant createdAt) {
        this.recipientId = m.recipientId();
        this.subject = Objects.requireNonNull(m.subject());
        this.body = Objects.requireNonNull(m.body());
        this.priority = m.priority();
        this.eventType = Objects.requireNonNull(m.eventType());
        this.incidentId = m.incidentId();
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean belongsTo(long userId) {
        return recipientId == userId;
    }

    public Long getId() {
        return id;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}

package in.ac.bitspilani.triagegraph.notification.domain;

import java.time.Instant;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One delivery attempt per recipient per channel, kept for the admin and for TC20. */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChannelType channel;

    @Column(nullable = false, length = 40)
    private String eventType;

    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus status;

    @Column(length = 500)
    private String error;

    @Column(nullable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(long recipientId, ChannelType channel, String eventType, Long incidentId,
            DeliveryStatus status, String error, Instant createdAt) {
        this.recipientId = recipientId;
        this.channel = channel;
        this.eventType = eventType;
        this.incidentId = incidentId;
        this.status = status;
        this.error = error == null || error.length() <= 500 ? error : error.substring(0, 500);
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

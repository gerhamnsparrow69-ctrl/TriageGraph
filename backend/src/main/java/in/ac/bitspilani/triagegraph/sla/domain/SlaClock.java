package in.ac.bitspilani.triagegraph.sla.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sla_clocks")
public class SlaClock {

    @Id
    @Column(name = "incident_id")
    private Long incidentId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "incident_code", nullable = false, length = 50)
    private String incidentCode;

    @Column(name = "service_id", nullable = false)
    private long serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(name = "response_started_at", nullable = false)
    private Instant responseStartedAt;

    @Column(name = "resolution_started_at", nullable = false)
    private Instant resolutionStartedAt;

    @Column(name = "response_active_millis", nullable = false)
    private long responseActiveMillis;

    @Column(name = "resolution_active_millis", nullable = false)
    private long resolutionActiveMillis;

    @Column(name = "response_active_since")
    private Instant responseActiveSince;

    @Column(name = "resolution_active_since")
    private Instant resolutionActiveSince;

    @Column(name = "response_paused_millis", nullable = false)
    private long responsePausedMillis;

    @Column(name = "resolution_paused_millis", nullable = false)
    private long resolutionPausedMillis;

    @Column(name = "response_paused_at")
    private Instant responsePausedAt;

    @Column(name = "resolution_paused_at")
    private Instant resolutionPausedAt;

    @Column(name = "response_warning_raised", nullable = false)
    private boolean responseWarningRaised;

    @Column(name = "response_breach_raised", nullable = false)
    private boolean responseBreachRaised;

    @Column(name = "resolution_warning_raised", nullable = false)
    private boolean resolutionWarningRaised;

    @Column(name = "resolution_breach_raised", nullable = false)
    private boolean resolutionBreachRaised;

    protected SlaClock() {
    }

    public SlaClock(IncidentSnapshot incident, Instant now) {
        this.incidentId = incident.id();
        this.incidentCode = incident.code();
        this.serviceId = incident.serviceId();
        this.priority = incident.priority();

        Instant start = Objects.requireNonNullElse(incident.createdAt(), now);
        this.responseStartedAt = start;
        this.resolutionStartedAt = start;

        if (isPausedStatus(incident.status())) {
            this.responsePausedAt = now;
            this.resolutionPausedAt = now;
        } else {
            this.responseActiveSince = start;
            this.resolutionActiveSince = start;
        }
    }

    public void update(IncidentSnapshot incident, Instant now) {
        this.incidentCode = incident.code();
        this.serviceId = incident.serviceId();
        if (this.priority != null && this.priority != incident.priority()) {
            resetForPriorityChange();
        }
        this.priority = incident.priority();
        syncStatus(SlaKind.RESPONSE, isPausedStatus(incident.status()), now);
        syncStatus(SlaKind.RESOLUTION, isPausedStatus(incident.status()), now);
    }

    public void applyPriorityChange(Priority priority) {
        if (this.priority != priority) {
            resetForPriorityChange();
            this.priority = priority;
        }
    }

    private void resetForPriorityChange() {
        responseWarningRaised = false;
        responseBreachRaised = false;
        resolutionWarningRaised = false;
        resolutionBreachRaised = false;
    }

    public long elapsedMillis(SlaKind kind, Instant now) {
        return activeMillis(kind, now);
    }

    public Instant dueAt(SlaKind kind, Duration target, Instant now) {
        Instant startedAt = kind == SlaKind.RESPONSE ? responseStartedAt : resolutionStartedAt;
        long pausedMillis = kind == SlaKind.RESPONSE ? responsePausedMillis : resolutionPausedMillis;
        Instant pausedAt = kind == SlaKind.RESPONSE ? responsePausedAt : resolutionPausedAt;

        long currentPauseMillis = pausedAt == null ? 0 :
                Math.max(0, Duration.between(pausedAt, now).toMillis());
        return startedAt.plus(target).plusMillis(pausedMillis + currentPauseMillis);
    }

    public boolean isWarningRaised(SlaKind kind) {
        return kind == SlaKind.RESPONSE ? responseWarningRaised : resolutionWarningRaised;
    }

    public boolean isBreachRaised(SlaKind kind) {
        return kind == SlaKind.RESPONSE ? responseBreachRaised : resolutionBreachRaised;
    }

    public void markWarningRaised(SlaKind kind) {
        if (kind == SlaKind.RESPONSE) {
            responseWarningRaised = true;
        } else {
            resolutionWarningRaised = true;
        }
    }

    public void markBreachRaised(SlaKind kind) {
        if (kind == SlaKind.RESPONSE) {
            responseBreachRaised = true;
        } else {
            resolutionBreachRaised = true;
        }
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public long getServiceId() {
        return serviceId;
    }

    public Priority getPriority() {
        return priority;
    }

    public Instant getStartedAt(SlaKind kind) {
        return kind == SlaKind.RESPONSE ? responseStartedAt : resolutionStartedAt;
    }

    public long getActiveMillis(SlaKind kind) {
        return kind == SlaKind.RESPONSE ? responseActiveMillis : resolutionActiveMillis;
    }

    public Instant getPausedAt(SlaKind kind) {
        return kind == SlaKind.RESPONSE ? responsePausedAt : resolutionPausedAt;
    }

    private long activeMillis(SlaKind kind, Instant now) {
        long accumulated = kind == SlaKind.RESPONSE ? responseActiveMillis : resolutionActiveMillis;
        Instant activeSince = kind == SlaKind.RESPONSE ? responseActiveSince : resolutionActiveSince;
        if (activeSince == null) {
            return accumulated;
        }
        return accumulated + Math.max(0, Duration.between(activeSince, now).toMillis());
    }

    private void syncStatus(SlaKind kind, boolean shouldPause, Instant now) {
        Instant activeSince = kind == SlaKind.RESPONSE ? responseActiveSince : resolutionActiveSince;
        Instant pausedAt = kind == SlaKind.RESPONSE ? responsePausedAt : resolutionPausedAt;

        if (shouldPause) {
            if (pausedAt == null) {
                long elapsed = activeSince == null ? 0 :
                        Math.max(0, Duration.between(activeSince, now).toMillis());
                if (kind == SlaKind.RESPONSE) {
                    responseActiveMillis += elapsed;
                    responseActiveSince = null;
                    responsePausedAt = now;
                } else {
                    resolutionActiveMillis += elapsed;
                    resolutionActiveSince = null;
                    resolutionPausedAt = now;
                }
            }
            return;
        }

        if (pausedAt != null) {
            long pauseDuration = Math.max(0, Duration.between(pausedAt, now).toMillis());
            if (kind == SlaKind.RESPONSE) {
                responsePausedMillis += pauseDuration;
                responsePausedAt = null;
                responseActiveSince = now;
            } else {
                resolutionPausedMillis += pauseDuration;
                resolutionPausedAt = null;
                resolutionActiveSince = now;
            }
        } else if (activeSince == null) {
            if (kind == SlaKind.RESPONSE) {
                responseActiveSince = now;
            } else {
                resolutionActiveSince = now;
            }
        }
    }

    public static boolean isPausedStatus(IncidentStatus status) {
        return status == IncidentStatus.PENDING_INFO || status == IncidentStatus.RESOLVED;
    }
}

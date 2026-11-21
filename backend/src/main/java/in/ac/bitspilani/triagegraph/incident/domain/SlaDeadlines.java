package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.Instant;

/**
 * The incident's SLA clock: due times, when it was first responded to, and pauses.
 * The clock pauses while waiting for the reporter (PENDING_INFO) or for verification (RESOLVED);
 * when it resumes, the resolution deadline moves out by the paused time.
 * <p>
 * Warning/breach detection is Member 3's SlaMonitor; this class only keeps the times.
 */
@Embeddable
public class SlaDeadlines {

    @Column(name = "sla_policy", length = 100)
    private String policyName;

    @Column(name = "sla_warning_percent")
    private Integer warningPercent;

    @Column(name = "sla_started_at")
    private Instant startedAt;

    @Column(name = "sla_response_due_at")
    private Instant responseDueAt;

    @Column(name = "sla_resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "sla_responded_at")
    private Instant respondedAt;

    @Column(name = "sla_paused_at")
    private Instant pausedAt;

    public SlaDeadlines() {
    }

    void start(SlaTargets targets, Instant now) {
        this.policyName = targets.policyName();
        this.warningPercent = targets.warningPercent();
        this.startedAt = now;
        this.responseDueAt = now.plus(targets.response());
        this.resolutionDueAt = now.plus(targets.resolution());
        this.respondedAt = null;
        this.pausedAt = null;
    }

    /** First acknowledgement counts; later ones (after a reassign) do not move it. */
    public void markResponded(Instant now) {
        if (respondedAt == null) {
            respondedAt = now;
        }
    }

    public void pause(Instant now) {
        if (pausedAt == null) {
            pausedAt = now;
        }
    }

    public void resume(Instant now) {
        if (pausedAt == null) {
            return;
        }
        Duration paused = Duration.between(pausedAt, now);
        if (resolutionDueAt != null && !paused.isNegative()) {
            resolutionDueAt = resolutionDueAt.plus(paused);
        }
        pausedAt = null;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isPaused() {
        return pausedAt != null;
    }

    public String getPolicyName() {
        return policyName;
    }

    public Integer getWarningPercent() {
        return warningPercent;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getResponseDueAt() {
        return responseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }
}

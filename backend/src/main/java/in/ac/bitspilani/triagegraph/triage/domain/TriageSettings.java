package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Admin-editable triage configuration (single row). */
@Entity
@Table(name = "triage_settings")
public class TriageSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int MAX_RESULTS = 5;

    @Id
    private Long id;

    @Version
    private long version;

    @Column(name = "likely_threshold", nullable = false)
    private double likelyThreshold;

    @Column(name = "possibly_threshold", nullable = false)
    private double possiblyThreshold;

    @Column(name = "window_hours", nullable = false)
    private int windowHours;

    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TriageSettings() {
    }

    public static TriageSettings initial(double likely, double possibly, int windowHours, boolean aiEnabled, Instant now) {
        TriageSettings s = new TriageSettings();
        s.id = SINGLETON_ID;
        s.apply(likely, possibly, windowHours, aiEnabled, null, now);
        return s;
    }

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException invalid thresholds or window */
    public void update(double likely, double possibly, int hours, boolean ai, Long actor, Instant now) {
        apply(likely, possibly, hours, ai, actor, now);
    }

    private void apply(double likely, double possibly, int hours, boolean ai, Long actor, Instant now) {
        new DuplicatePolicy(likely, possibly, Duration.ofHours(hours), MAX_RESULTS); // validates
        this.likelyThreshold = likely;
        this.possiblyThreshold = possibly;
        this.windowHours = hours;
        this.aiEnabled = ai;
        this.updatedBy = actor;
        this.updatedAt = now;
    }

    public DuplicatePolicy policy() {
        return new DuplicatePolicy(likelyThreshold, possiblyThreshold, Duration.ofHours(windowHours), MAX_RESULTS);
    }

    public double getLikelyThreshold() {
        return likelyThreshold;
    }

    public double getPossiblyThreshold() {
        return possiblyThreshold;
    }

    public int getWindowHours() {
        return windowHours;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

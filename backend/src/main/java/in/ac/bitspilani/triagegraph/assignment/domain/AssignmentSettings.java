package in.ac.bitspilani.triagegraph.assignment.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** The single row of assignment configuration: which strategy is active, who changed it and when. */
@Entity
@Table(name = "assignment_settings")
public class AssignmentSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Version
    private long version;

    @Column(name = "active_strategy", nullable = false, length = 40)
    private String activeStrategy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssignmentSettings() {
    }

    private AssignmentSettings(String activeStrategy, Instant now) {
        this.id = SINGLETON_ID;
        this.activeStrategy = Objects.requireNonNull(activeStrategy);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public static AssignmentSettings initial(String strategyKey, Instant now) {
        return new AssignmentSettings(strategyKey, now);
    }

    /** Caller has already checked that the key exists in the registry. */
    public void switchTo(String strategyKey, Long actorId, Instant now) {
        this.activeStrategy = Objects.requireNonNull(strategyKey);
        this.updatedBy = actorId;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public String getActiveStrategy() {
        return activeStrategy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

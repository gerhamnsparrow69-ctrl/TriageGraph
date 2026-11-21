package in.ac.bitspilani.triagegraph.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

/** What the technician did to fix the incident. Part of the incident (composition). */
@Embeddable
public class Resolution {

    @Column(name = "resolution_summary", length = 2000)
    private String summary;

    @Column(name = "resolution_root_cause", length = 1000)
    private String rootCause;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    protected Resolution() {
    }

    public Resolution(String summary, String rootCause, Instant resolvedAt, Long resolvedById) {
        this.summary = Objects.requireNonNull(summary);
        this.rootCause = rootCause;
        this.resolvedAt = Objects.requireNonNull(resolvedAt);
        this.resolvedById = resolvedById;
    }

    public String getSummary() {
        return summary;
    }

    public String getRootCause() {
        return rootCause;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getResolvedById() {
        return resolvedById;
    }
}

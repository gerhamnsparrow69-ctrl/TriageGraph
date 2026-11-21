package in.ac.bitspilani.triagegraph.reporting.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** Test builder for IncidentFact. Defaults: P3 NETWORK incident on service 1, open, reported at T0. */
public final class Facts {

    public static final Instant T0 = Instant.parse("2026-11-20T04:30:00Z"); // 10:00 IST

    private long id = 1;
    private String title = "Wi-Fi down in hostel";
    private IncidentStatus status = IncidentStatus.ASSIGNED;
    private Priority priority = Priority.P3;
    private String category = "NETWORK";
    private Long serviceId = 1L;
    private long reporterId = 100;
    private Long assigneeId = 200L;
    private String source = "KNN";
    private Instant createdAt = T0;
    private Instant respondedAt;
    private Instant resolvedAt;
    private Instant resolutionDueAt = T0.plus(Duration.ofHours(24));
    private Instant pausedAt;
    private Long majorId;
    private String rootCause;
    private Set<Long> followers = Set.of();

    public static Facts incident(long id) {
        Facts f = new Facts();
        f.id = id;
        return f;
    }

    public Facts title(String v) { title = v; return this; }
    public Facts status(IncidentStatus v) { status = v; return this; }
    public Facts priority(Priority v) { priority = v; return this; }
    public Facts category(String v) { category = v; return this; }
    public Facts service(Long v) { serviceId = v; return this; }
    public Facts reporter(long v) { reporterId = v; return this; }
    public Facts assignee(Long v) { assigneeId = v; return this; }
    public Facts source(String v) { source = v; return this; }
    public Facts created(Instant v) { createdAt = v; return this; }
    public Facts responded(Instant v) { respondedAt = v; return this; }
    public Facts due(Instant v) { resolutionDueAt = v; return this; }
    public Facts paused(Instant v) { pausedAt = v; return this; }
    public Facts major(Long v) { majorId = v; return this; }
    public Facts rootCause(String v) { rootCause = v; return this; }
    public Facts followers(Set<Long> v) { followers = v; return this; }

    public Facts resolved(Instant at) {
        resolvedAt = at;
        status = IncidentStatus.RESOLVED;
        return this;
    }

    public Facts closed(Instant resolved) {
        resolved(resolved);
        status = IncidentStatus.CLOSED;
        return this;
    }

    public IncidentFact build() {
        return new IncidentFact(id, "INC-" + (1000 + id), title, title + " since morning", status, priority, category,
                serviceId, serviceId == null ? null : "Service " + serviceId, reporterId, "Reporter " + reporterId,
                assigneeId, assigneeId == null ? null : "Tech " + assigneeId, source, createdAt, respondedAt,
                resolvedAt, createdAt.plus(Duration.ofHours(1)), resolutionDueAt, createdAt, pausedAt, 75, majorId,
                null, rootCause, followers);
    }
}

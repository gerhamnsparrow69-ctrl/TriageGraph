package in.ac.bitspilani.triagegraph.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

/** When and by whom the incident was closed ({@code closedById} null = closed automatically). */
@Embeddable
public class Closure {

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_id")
    private Long closedById;

    protected Closure() {
    }

    public Closure(Instant closedAt, Long closedById) {
        this.closedAt = Objects.requireNonNull(closedAt);
        this.closedById = closedById;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Long getClosedById() {
        return closedById;
    }
}

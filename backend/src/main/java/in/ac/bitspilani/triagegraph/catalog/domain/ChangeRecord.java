package in.ac.bitspilani.triagegraph.catalog.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "change_records")
public class ChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ITService service;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Instant loggedAt;

    protected ChangeRecord() {
    }

    public ChangeRecord(ITService service, String description, Instant loggedAt) {
        this.service = Objects.requireNonNull(service);
        this.description = Objects.requireNonNull(description);
        this.loggedAt = Objects.requireNonNull(loggedAt);
    }

    public Long getId() {
        return id;
    }

    public ITService getService() {
        return service;
    }

    public String getDescription() {
        return description;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }
}
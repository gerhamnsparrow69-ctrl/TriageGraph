package in.ac.bitspilani.triagegraph.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A comment or work note on an incident. Owned by the incident (composition: cascade + orphan
 * removal), so it is only ever created through {@link Incident#addNote}.
 * Internal notes are visible to IT staff only.
 */
@Entity
@Table(name = "investigation_notes")
public class InvestigationNote {

    public static final int MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    private Long authorId;

    private boolean internal;

    @Column(nullable = false)
    private Instant createdAt;

    protected InvestigationNote() {
    }

    InvestigationNote(String text, Long authorId, boolean internal, Instant createdAt) {
        this.text = Objects.requireNonNull(text);
        this.authorId = authorId;
        this.internal = internal;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public boolean isInternal() {
        return internal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

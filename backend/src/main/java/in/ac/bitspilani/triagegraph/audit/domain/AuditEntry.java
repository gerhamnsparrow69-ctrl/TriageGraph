package in.ac.bitspilani.triagegraph.audit.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;

/**
 * One link in the tamper-evident audit chain. Immutable: no setters, Hibernate never issues an UPDATE
 * ({@link Immutable}), and the repository has no update or delete. Each entry stores the previous
 * entry's hash, so changing or removing any row breaks every hash after it.
 */
@Entity
@Immutable
@Table(name = "audit_entries", indexes = @Index(name = "ix_audit_incident", columnList = "incidentId, id"))
public class AuditEntry {

    public static final int MAX_DESCRIPTION = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant occurredAt;

    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    private Long incidentId;

    @Column(length = 30)
    private String incidentCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentStatus toState;

    @Column(length = MAX_DESCRIPTION)
    private String description;

    @Column(nullable = false, length = 64)
    private String prevHash;

    @Column(nullable = false, length = 64, unique = true)
    private String hash;

    protected AuditEntry() {
    }

    private AuditEntry(AuditDraft d, String prevHash) {
        this.occurredAt = AuditHasher.normalise(d.occurredAt());
        this.actorId = d.actorId();
        this.actorRole = d.actorRole();
        this.action = Objects.requireNonNull(d.action());
        this.incidentId = d.incidentId();
        this.incidentCode = d.incidentCode();
        this.fromState = d.fromState();
        this.toState = d.toState();
        this.description = truncate(d.description());
        this.prevHash = Objects.requireNonNull(prevHash);
        this.hash = computeHash();
    }

    /** Links {@code draft} after the entry whose hash is {@code prevHash}. */
    public static AuditEntry chain(AuditDraft draft, String prevHash) {
        return new AuditEntry(draft, prevHash);
    }

    /** Recomputes the hash from the stored fields; differs from {@link #getHash()} if a row was edited. */
    public String computeHash() {
        return AuditHasher.hash(prevHash, incidentId, actorId, action, fromState, toState, description, occurredAt);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_DESCRIPTION ? s : s.substring(0, MAX_DESCRIPTION - 1) + "…";
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getActorId() {
        return actorId;
    }

    public Role getActorRole() {
        return actorRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public IncidentStatus getFromState() {
        return fromState;
    }

    public IncidentStatus getToState() {
        return toState;
    }

    public String getDescription() {
        return description;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getHash() {
        return hash;
    }
}

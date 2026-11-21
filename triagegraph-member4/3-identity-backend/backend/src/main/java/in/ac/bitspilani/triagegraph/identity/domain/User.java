package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Base of the user hierarchy. Each subclass decides what it may see and do, so object-level
 * authorisation is polymorphic instead of a switch on role.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {

    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCKOUT = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String pushTopic;

    private boolean active = true;

    private int failedLogins;

    private Instant lockedUntil;

    @Column(nullable = false)
    private Instant createdAt;

    protected User() {
    }

    protected User(String name, String email, String passwordHash, String department, Instant createdAt) {
        this.name = Objects.requireNonNull(name);
        this.email = normaliseEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.department = department;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public abstract Role role();

    /** Whether this user may open the incident at all. */
    public abstract boolean canView(IncidentSnapshot incident);

    /** Whether this user may perform the action on the incident (state rules are checked separately). */
    public abstract boolean canPerform(IncidentAction action, IncidentSnapshot incident);

    public static String normaliseEmail(String email) {
        return Objects.requireNonNull(email).trim().toLowerCase();
    }

    protected boolean isReporterOrFollower(IncidentSnapshot incident) {
        return incident.reporterId() == id
                || (incident.followerIds() != null && incident.followerIds().contains(id));
    }

    protected boolean isAssignee(IncidentSnapshot incident) {
        return incident.assigneeId() != null && incident.assigneeId().equals(id);
    }

    // --- login protection ---

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** Counts a wrong password. The fifth in a row locks the account for {@link #LOCKOUT}. */
    public void recordFailedLogin(Instant now) {
        failedLogins++;
        if (failedLogins >= MAX_FAILED_LOGINS) {
            lockedUntil = now.plus(LOCKOUT);
            failedLogins = 0;
        }
    }

    public void recordSuccessfulLogin() {
        failedLogins = 0;
        lockedUntil = null;
    }

    // --- profile changes ---

    public void changePassword(String newHash) {
        this.passwordHash = Objects.requireNonNull(newHash);
    }

    public void updateProfile(String name, String department, String pushTopic) {
        this.name = Objects.requireNonNull(name);
        this.department = department;
        this.pushTopic = pushTopic;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public UserRef toRef() {
        return new UserRef(id, name, role(), email, pushTopic);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDepartment() {
        return department;
    }

    public String getPushTopic() {
        return pushTopic;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

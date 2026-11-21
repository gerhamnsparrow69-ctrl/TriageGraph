package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.identity.domain.Administrator;
import in.ac.bitspilani.triagegraph.identity.domain.Employee;
import in.ac.bitspilani.triagegraph.identity.domain.ItManager;
import in.ac.bitspilani.triagegraph.identity.domain.TeamLead;
import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;

/** Factory pattern: the one place that knows which subclass each role maps to. Hashes the password. */
@Component
public class UserFactory {

    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final Clock clock;

    public UserFactory(PasswordEncoder encoder, PasswordPolicy policy, Clock clock) {
        this.encoder = encoder;
        this.policy = policy;
        this.clock = clock;
    }

    public User create(NewUser u) {
        policy.check(u.rawPassword());
        String hash = encoder.encode(u.rawPassword());
        Instant now = Instant.now(clock);
        return switch (u.role()) {
            case EMPLOYEE -> new Employee(u.name(), u.email(), hash, u.department(), now);
            case TECHNICIAN -> new Technician(u.name(), u.email(), hash, u.department(), now,
                    u.skills(), u.experienceYears(), u.maxLoad());
            case TEAM_LEAD -> new TeamLead(u.name(), u.email(), hash, u.department(), now,
                    u.skills(), u.experienceYears(), u.maxLoad());
            case ADMIN -> new Administrator(u.name(), u.email(), hash, u.department(), now);
            case IT_MANAGER -> new ItManager(u.name(), u.email(), hash, u.department(), now);
        };
    }
}

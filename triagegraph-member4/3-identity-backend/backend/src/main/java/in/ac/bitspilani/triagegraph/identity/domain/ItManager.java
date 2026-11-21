package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Instant;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Head of IT: same powers as an administrator, plus the final escalation level and major-incident alerts. */
@Entity
@DiscriminatorValue("IT_MANAGER")
public class ItManager extends Administrator {

    protected ItManager() {
    }

    public ItManager(String name, String email, String passwordHash, String department, Instant createdAt) {
        super(name, email, passwordHash, department, createdAt);
    }

    @Override
    public Role role() {
        return Role.IT_MANAGER;
    }
}

package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** System administrator: sees everything and manages users, catalog and policies. */
@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    /** Only the reporter can confirm the fix or rate it. */
    private static final Set<IncidentAction> REPORTER_ONLY = EnumSet.of(IncidentAction.VERIFY, IncidentAction.FEEDBACK);

    protected Administrator() {
    }

    public Administrator(String name, String email, String passwordHash, String department, Instant createdAt) {
        super(name, email, passwordHash, department, createdAt);
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }

    @Override
    public boolean canView(IncidentSnapshot incident) {
        return true;
    }

    @Override
    public boolean canPerform(IncidentAction action, IncidentSnapshot incident) {
        return !REPORTER_ONLY.contains(action);
    }
}

package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Students and staff who report incidents. They only see incidents they reported or joined. */
@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    private static final Set<IncidentAction> REPORTER_ACTIONS = EnumSet.of(
            IncidentAction.PROVIDE_INFO, IncidentAction.ADD_NOTE, IncidentAction.VERIFY,
            IncidentAction.REOPEN, IncidentAction.FEEDBACK);

    protected Employee() {
    }

    public Employee(String name, String email, String passwordHash, String department, Instant createdAt) {
        super(name, email, passwordHash, department, createdAt);
    }

    @Override
    public Role role() {
        return Role.EMPLOYEE;
    }

    @Override
    public boolean canView(IncidentSnapshot incident) {
        return isReporterOrFollower(incident);
    }

    @Override
    public boolean canPerform(IncidentAction action, IncidentSnapshot incident) {
        if (!canView(incident)) {
            return false;
        }
        // Followers who joined as duplicates can comment, but only the reporter verifies, reopens or rates.
        if (incident.reporterId() != getId()) {
            return action == IncidentAction.ADD_NOTE;
        }
        return REPORTER_ACTIONS.contains(action);
    }
}

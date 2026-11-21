package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A senior technician who also supervises a team: first escalation target, can reassign and merge.
 * Team membership is checked by the caller via {@link #leads(Long)} since incidents only carry ids.
 */
@Entity
@DiscriminatorValue("TEAM_LEAD")
public class TeamLead extends Technician {

    private static final Set<IncidentAction> LEAD_ACTIONS = EnumSet.of(
            IncidentAction.ASSIGN, IncidentAction.ESCALATE, IncidentAction.MERGE, IncidentAction.ADD_NOTE);

    protected TeamLead() {
    }

    public TeamLead(String name, String email, String passwordHash, String department, Instant createdAt,
            Set<String> skills, int experienceYears, int maxLoad) {
        super(name, email, passwordHash, department, createdAt, skills, experienceYears, maxLoad);
    }

    @Override
    public Role role() {
        return Role.TEAM_LEAD;
    }

    /** Team leads see every incident so they can watch their team's queue and pick up escalations. */
    @Override
    public boolean canView(IncidentSnapshot incident) {
        return true;
    }

    @Override
    public boolean canPerform(IncidentAction action, IncidentSnapshot incident) {
        return super.canPerform(action, incident) || LEAD_ACTIONS.contains(action);
    }

    public boolean leads(Long technicianLeadId) {
        return technicianLeadId != null && technicianLeadId.equals(getId());
    }
}

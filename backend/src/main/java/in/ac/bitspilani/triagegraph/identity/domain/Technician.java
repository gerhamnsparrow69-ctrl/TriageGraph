package in.ac.bitspilani.triagegraph.identity.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Works on assigned incidents. Skills, experience and max load feed the assignment strategies. */
@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    private static final Set<IncidentAction> ASSIGNEE_ACTIONS = EnumSet.of(
            IncidentAction.ACKNOWLEDGE, IncidentAction.START_WORK, IncidentAction.REQUEST_INFO,
            IncidentAction.RESOLVE, IncidentAction.ADD_NOTE, IncidentAction.ESCALATE);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill", length = 40)
    private Set<String> skills = new LinkedHashSet<>();

    private int experienceYears;

    private int maxLoad;

    private boolean available = true;

    // Eager: responses show the lead's name, and open-in-view is off.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_lead_id")
    private TeamLead teamLead;

    protected Technician() {
    }

    public Technician(String name, String email, String passwordHash, String department, Instant createdAt,
            Set<String> skills, int experienceYears, int maxLoad) {
        super(name, email, passwordHash, department, createdAt);
        changeSkills(skills);
        changeCapacity(experienceYears, maxLoad);
    }

    @Override
    public Role role() {
        return Role.TECHNICIAN;
    }

    @Override
    public boolean canView(IncidentSnapshot incident) {
        return isAssignee(incident) || isReporterOrFollower(incident);
    }

    @Override
    public boolean canPerform(IncidentAction action, IncidentSnapshot incident) {
        return isAssignee(incident) && ASSIGNEE_ACTIONS.contains(action);
    }

    public void changeSkills(Set<String> newSkills) {
        Objects.requireNonNull(newSkills);
        skills.clear();
        newSkills.stream().map(s -> s.trim().toUpperCase()).forEach(skills::add);
    }

    public void changeCapacity(int experienceYears, int maxLoad) {
        if (experienceYears < 0 || maxLoad < 1) {
            throw new IllegalArgumentException("experience must be >= 0 and max load >= 1");
        }
        this.experienceYears = experienceYears;
        this.maxLoad = maxLoad;
    }

    public void reportTo(TeamLead lead) {
        if (lead == this) {
            throw new IllegalArgumentException("a team lead cannot report to themselves");
        }
        this.teamLead = lead;
    }

    public void markAvailable(boolean available) {
        this.available = available;
    }

    public boolean hasSkill(String skill) {
        return skills.contains(skill.toUpperCase());
    }

    public TechnicianProfile toProfile(int activeLoad, Map<Long, Integer> fixesByService) {
        return new TechnicianProfile(getId(), getName(), Set.copyOf(skills), activeLoad, maxLoad,
                experienceYears, Map.copyOf(fixesByService), available && isActive());
    }

    public Set<String> getSkills() {
        return Set.copyOf(skills);
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    public boolean isAvailable() {
        return available;
    }

    public TeamLead getTeamLead() {
        return teamLead;
    }
}

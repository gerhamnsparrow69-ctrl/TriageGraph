package in.ac.bitspilani.triagegraph.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

class UserHierarchyTest {

    private static final Instant T0 = Instant.parse("2026-11-21T09:00:00Z");

    private final Employee dev = withId(new Employee("Dev Sharma", "Dev@Campus.test ", "h", "Finance", T0), 1);
    private final Employee rahul = withId(new Employee("Rahul Verma", "rahul@campus.test", "h", null, T0), 2);
    private final Employee fatima = withId(new Employee("Fatima Khan", "fatima@campus.test", "h", null, T0), 3);
    private final Technician sneha = withId(new Technician("Sneha Reddy", "sneha@campus.test", "h", null, T0,
            Set.of("application", "DATABASE"), 4, 6), 10);
    private final Technician ravi = withId(new Technician("Ravi Kumar", "ravi@campus.test", "h", null, T0,
            Set.of("NETWORK"), 5, 6), 11);
    private final TeamLead ananya = withId(new TeamLead("Ananya Iyer", "ananya@campus.test", "h", null, T0,
            Set.of("APPLICATION"), 8, 4), 20);
    private final Administrator vikram = withId(new Administrator("Vikram Singh", "vikram@campus.test", "h", null, T0), 30);
    private final ItManager meera = withId(new ItManager("Meera Joshi", "meera@campus.test", "h", null, T0), 31);

    /** Reported by Dev, joined by Rahul, assigned to Sneha. */
    private final IncidentSnapshot incident = new IncidentSnapshot(100, "INC-0100", "Fee payment fails", "500 on checkout",
            IncidentStatus.ASSIGNED, Priority.P1, 1, "APPLICATION", 1, Set.of(2L), 10L, null, T0, T0, T0);

    @Test
    void eachSubclassReportsItsRole() {
        assertThat(dev.role()).isEqualTo(Role.EMPLOYEE);
        assertThat(sneha.role()).isEqualTo(Role.TECHNICIAN);
        assertThat(ananya.role()).isEqualTo(Role.TEAM_LEAD);
        assertThat(vikram.role()).isEqualTo(Role.ADMIN);
        assertThat(meera.role()).isEqualTo(Role.IT_MANAGER);
    }

    @Test
    void emailIsNormalisedAndSkillsUpperCased() {
        assertThat(dev.getEmail()).isEqualTo("dev@campus.test");
        assertThat(sneha.getSkills()).containsExactlyInAnyOrder("APPLICATION", "DATABASE");
        assertThat(sneha.hasSkill("database")).isTrue();
    }

    @Test
    void employeesOnlySeeTheirOwnOrJoinedIncidents() {
        assertThat(dev.canView(incident)).isTrue();
        assertThat(rahul.canView(incident)).isTrue();
        assertThat(fatima.canView(incident)).isFalse();
    }

    @Test
    void onlyTheReporterCanVerifyOrRate() {
        assertThat(dev.canPerform(IncidentAction.VERIFY, incident)).isTrue();
        assertThat(dev.canPerform(IncidentAction.FEEDBACK, incident)).isTrue();
        assertThat(rahul.canPerform(IncidentAction.VERIFY, incident)).isFalse();
        assertThat(rahul.canPerform(IncidentAction.ADD_NOTE, incident)).isTrue();
        assertThat(dev.canPerform(IncidentAction.RESOLVE, incident)).isFalse();
    }

    @Test
    void onlyTheAssigneeTechnicianCanWorkTheIncident() {
        assertThat(sneha.canPerform(IncidentAction.RESOLVE, incident)).isTrue();
        assertThat(ravi.canView(incident)).isFalse();
        assertThat(ravi.canPerform(IncidentAction.RESOLVE, incident)).isFalse();
    }

    @Test
    void teamLeadSeesAllAndCanReassignButNotResolveOthersWork() {
        assertThat(ananya.canView(incident)).isTrue();
        assertThat(ananya.canPerform(IncidentAction.ASSIGN, incident)).isTrue();
        assertThat(ananya.canPerform(IncidentAction.ESCALATE, incident)).isTrue();
        assertThat(ananya.canPerform(IncidentAction.RESOLVE, incident)).isFalse();
    }

    @Test
    void adminsDoEverythingExceptReporterOnlyActions() {
        assertThat(vikram.canView(incident)).isTrue();
        assertThat(vikram.canPerform(IncidentAction.CLOSE, incident)).isTrue();
        assertThat(vikram.canPerform(IncidentAction.VERIFY, incident)).isFalse();
        assertThat(meera.canPerform(IncidentAction.MERGE, incident)).isTrue();
    }

    @Test
    void fifthWrongPasswordLocksForFiveMinutes() {
        for (int i = 0; i < User.MAX_FAILED_LOGINS - 1; i++) {
            dev.recordFailedLogin(T0);
            assertThat(dev.isLocked(T0)).isFalse();
        }
        dev.recordFailedLogin(T0);
        assertThat(dev.isLocked(T0)).isTrue();
        assertThat(dev.isLocked(T0.plusSeconds(299))).isTrue();
        assertThat(dev.isLocked(T0.plusSeconds(300))).isFalse();
    }

    @Test
    void successfulLoginClearsTheCounter() {
        dev.recordFailedLogin(T0);
        dev.recordFailedLogin(T0);
        dev.recordSuccessfulLogin();
        assertThat(dev.getFailedLogins()).isZero();
    }

    @Test
    void capacityAndTeamRulesAreEnforced() {
        assertThatThrownBy(() -> sneha.changeCapacity(4, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ananya.reportTo(ananya)).isInstanceOf(IllegalArgumentException.class);
        sneha.reportTo(ananya);
        assertThat(sneha.getTeamLead()).isSameAs(ananya);
        assertThat(sneha.toProfile(2, java.util.Map.of()).activeLoad()).isEqualTo(2);
    }

    private static <T extends User> T withId(T user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

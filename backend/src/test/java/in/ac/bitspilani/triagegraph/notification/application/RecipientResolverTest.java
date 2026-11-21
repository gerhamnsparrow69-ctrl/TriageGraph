package in.ac.bitspilani.triagegraph.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationTrigger;
import in.ac.bitspilani.triagegraph.notification.domain.Recipient;
import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.NoteAdded;
import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;

class RecipientResolverTest {

    static final Instant T = Instant.parse("2026-11-21T10:00:00Z");

    static final Map<Long, UserRef> USERS = new HashMap<>(Map.of(
            1L, new UserRef(1, "Dev Sharma", Role.EMPLOYEE, "dev@campus.test", null),
            2L, new UserRef(2, "Rahul Verma", Role.EMPLOYEE, "rahul@campus.test", null),
            10L, new UserRef(10, "Sneha Reddy", Role.TECHNICIAN, "sneha@campus.test", "tg-sneha"),
            20L, new UserRef(20, "Ananya Iyer", Role.TEAM_LEAD, "ananya@campus.test", "tg-ananya")));

    static final UserDirectory DIRECTORY = new UserDirectory() {
        @Override
        public UserRef user(long id) {
            return USERS.get(id);
        }

        @Override
        public List<TechnicianProfile> technicians() {
            return List.of();
        }

        @Override
        public Optional<UserRef> teamLeadOf(long technicianId) {
            return technicianId == 10 ? Optional.of(USERS.get(20L)) : Optional.empty();
        }

        @Override
        public List<UserRef> withRole(Role role) {
            return USERS.values().stream().filter(u -> u.role() == role).toList();
        }
    };

    /** INC-0042: reported by Dev, joined by Rahul, assigned to Sneha, P1. */
    static final IncidentSnapshot INCIDENT = new IncidentSnapshot(42, "INC-0042", "Fee payment fails", "500 on checkout",
            IncidentStatus.ASSIGNED, Priority.P1, 1, "APPLICATION", 1, Set.of(2L), 10L, null, T, T, T);

    static final IncidentQueryPort INCIDENTS = new IncidentQueryPort() {
        @Override
        public Optional<IncidentSnapshot> find(long id) {
            return id == 42 ? Optional.of(INCIDENT) : Optional.empty();
        }

        @Override
        public List<IncidentSnapshot> openSince(Instant since) {
            return List.of(INCIDENT);
        }

        @Override
        public List<IncidentSnapshot> openWithDeadlineBefore(Instant t) {
            return List.of();
        }
    };

    private final RecipientResolver withIncidents = resolver(true);
    private final RecipientResolver withoutIncidents = resolver(false);

    static RecipientResolver resolver(boolean incidentModulePresent) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (incidentModulePresent) {
            beans.addBean("incidents", INCIDENTS);
        }
        return new RecipientResolver(DIRECTORY, beans.getBeanProvider(IncidentQueryPort.class));
    }

    @Test
    void assignmentTellsTheTechnicianAndTheReporterWithIncidentDetails() {
        NotificationTrigger t = withIncidents.resolve(
                new IncidentAssigned(42, "INC-0042", 99L, T, null, 10, "SMART")).orElseThrow();
        assertThat(t.priority()).isEqualTo(Priority.P1);
        assertThat(t.incidentTitle()).isEqualTo("Fee payment fails");
        assertThat(t.recipients()).extracting(Recipient::role)
                .containsExactly(RecipientRole.ASSIGNEE, RecipientRole.REPORTER);
    }

    @Test
    void withoutTheIncidentModuleTheAssigneeIsStillNotified() {
        NotificationTrigger t = withoutIncidents.resolve(
                new IncidentAssigned(42, "INC-0042", 99L, T, null, 10, "SMART")).orElseThrow();
        assertThat(t.recipients()).extracting(r -> r.user().name()).containsExactly("Sneha Reddy");
        assertThat(t.priority()).isNull();
    }

    @Test
    void resolvedGoesToReporterAndFollowers() {
        NotificationTrigger t = withIncidents.resolve(new IncidentStatusChanged(42, "INC-0042", 10L, T,
                IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, "Restarted the payment gateway")).orElseThrow();
        assertThat(t.eventType()).isEqualTo(NotificationEvents.RESOLVED);
        assertThat(t.recipients()).extracting(r -> r.user().id()).containsExactly(1L, 2L);
    }

    @Test
    void breachAddsTheAssigneesTeamLead() {
        NotificationTrigger t = withIncidents.resolve(
                new SlaBreached(42, "INC-0042", null, T, SlaKind.RESPONSE, T)).orElseThrow();
        assertThat(t.recipients()).extracting(Recipient::role)
                .containsExactly(RecipientRole.ASSIGNEE, RecipientRole.TEAM_LEAD);
    }

    @Test
    void quietEventsProduceNothing() {
        assertThat(withIncidents.resolve(new NoteAdded(42, "INC-0042", 10L, T, 7, true))).isEmpty();
        assertThat(withIncidents.resolve(new PriorityAssigned(42, "INC-0042", null, T, null, Priority.P1, 83.2, "x")))
                .as("the first priority is part of reporting, not a change").isEmpty();
        assertThat(withIncidents.resolve(new IncidentStatusChanged(42, "INC-0042", 10L, T,
                IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, null))).isEmpty();
    }
}

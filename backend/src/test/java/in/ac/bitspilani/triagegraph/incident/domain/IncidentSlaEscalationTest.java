package in.ac.bitspilani.triagegraph.incident.domain;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEscalated;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaWarningRaised;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** SLA warnings, breaches and escalation recorded on the incident (raised by the SLA monitor). */
class IncidentSlaEscalationTest {

    static final Instant LATER = T0.plusSeconds(3600);

    @Test
    void dueAtResolvesEachKindFromTheSlaClock() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);
        assertThat(i.dueAt(SlaKind.RESPONSE)).isEqualTo(T0.plus(P1_SLA.response()));
        assertThat(i.dueAt(SlaKind.RESOLUTION)).isEqualTo(T0.plus(P1_SLA.resolution()));
    }

    @Test
    void aWarningRaisesSlaWarningRaisedWithTheDueTime() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);

        i.markSlaWarning(SlaKind.RESPONSE, LATER);

        assertThat(i.getUpdatedAt()).isEqualTo(LATER);
        assertThat(pull(i)).singleElement().isInstanceOfSatisfying(SlaWarningRaised.class, e -> {
            assertThat(e.incidentId()).isEqualTo(i.getId());
            assertThat(e.incidentCode()).isEqualTo(i.getCode());
            assertThat(e.actorId()).isNull();
            assertThat(e.occurredAt()).isEqualTo(LATER);
            assertThat(e.kind()).isEqualTo(SlaKind.RESPONSE);
            assertThat(e.dueAt()).isEqualTo(T0.plus(P1_SLA.response()));
        });
    }

    @Test
    void aBreachRaisesSlaBreachedWithTheDueTime() {
        Incident i = inStatus(IncidentStatus.IN_PROGRESS);

        i.markSlaBreached(SlaKind.RESOLUTION, LATER);

        assertThat(i.getUpdatedAt()).isEqualTo(LATER);
        assertThat(pull(i)).singleElement().isInstanceOfSatisfying(SlaBreached.class, e -> {
            assertThat(e.kind()).isEqualTo(SlaKind.RESOLUTION);
            assertThat(e.dueAt()).isEqualTo(i.sla().getResolutionDueAt());
        });
    }

    @Test
    void escalationRaisesTheLevelAndNeverLowersIt() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);

        i.escalate(2, LEAD_ID, "  Unacknowledged P1  ", LATER);
        assertThat(i.getEscalationLevel()).isEqualTo(2);
        assertThat(i.getUpdatedAt()).isEqualTo(LATER);
        assertThat(pull(i)).singleElement().isInstanceOfSatisfying(IncidentEscalated.class, e -> {
            assertThat(e.level()).isEqualTo(2);
            assertThat(e.targetUserId()).isEqualTo(LEAD_ID);
            assertThat(e.reason()).isEqualTo("Unacknowledged P1");
        });

        i.escalate(1, LEAD_ID, null, LATER.plusSeconds(60));
        assertThat(i.getEscalationLevel()).isEqualTo(2);
        assertThat(pull(i)).singleElement().isInstanceOf(IncidentEscalated.class);
    }

    @Test
    void escalationLevelMustBeAtLeastOne() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);
        assertThatThrownBy(() -> i.escalate(0, LEAD_ID, "x", LATER)).isInstanceOf(InvalidInputException.class);
        assertThat(i.getEscalationLevel()).isZero();
        assertThat(i.hasPendingEvents()).isFalse();
    }

    @Test
    void aClosedIncidentRejectsSlaEventsAndEscalation() {
        Incident i = inStatus(IncidentStatus.CLOSED);
        assertThatThrownBy(() -> i.markSlaWarning(SlaKind.RESPONSE, LATER))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> i.markSlaBreached(SlaKind.RESOLUTION, LATER))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> i.escalate(1, LEAD_ID, "x", LATER))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(i.hasPendingEvents()).isFalse();
    }

    @Test
    void slaEventsDoNotDisturbJoiningAsAffected() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);
        i.markSlaWarning(SlaKind.RESPONSE, LATER);
        int before = i.getAffectedUsers();

        assertThat(i.joinAsAffected(11L, OTHER_EMPLOYEE, LATER)).isTrue();
        assertThat(i.getAffectedUsers()).isEqualTo(before + 1);
        assertThat(pull(i)).hasSize(2);
    }
}

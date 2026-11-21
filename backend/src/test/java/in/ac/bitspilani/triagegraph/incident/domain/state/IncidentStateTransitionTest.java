package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.ClassificationInfo;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction.*;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The transition table (blueprint §6, BRL1–BRL8) as tests. Every (status, action) pair that is not in
 * the table must throw InvalidStateTransitionException and leave the incident untouched (TC16, TC17);
 * every pair that is in the table must reach the documented status.
 */
class IncidentStateTransitionTest {

    /** The legal actions per status, written out independently of the state classes. */
    static final Map<IncidentStatus, Set<IncidentAction>> LEGAL = new EnumMap<>(Map.of(
            NEW, EnumSet.of(CLASSIFY, MERGE),
            CLASSIFIED, EnumSet.of(ASSIGN, MERGE),
            ASSIGNED, EnumSet.of(ACKNOWLEDGE, ASSIGN),
            ACKNOWLEDGED, EnumSet.of(START_WORK, ASSIGN),
            IN_PROGRESS, EnumSet.of(REQUEST_INFO, RESOLVE),
            PENDING_INFO, EnumSet.of(PROVIDE_INFO),
            RESOLVED, EnumSet.of(VERIFY, REOPEN),
            VERIFIED, EnumSet.of(CLOSE),
            CLOSED, EnumSet.of(FEEDBACK),
            MERGED, EnumSet.noneOf(IncidentAction.class)));

    /** Actions that are lifecycle transitions (ADD_NOTE and ESCALATE are not). */
    static final Set<IncidentAction> TRANSITIONS = EnumSet.complementOf(EnumSet.of(ADD_NOTE, ESCALATE));

    private static final Instant LATER = T0.plusSeconds(5_000);

    /** Each action performed by the person entitled to it, so only the state can say no. */
    static Consumer<Incident> perform(IncidentAction action) {
        return switch (action) {
            case CLASSIFY -> i -> i.classify("NETWORK", 3, new ClassificationInfo("USER", 1, "x"), Actor.system(), LATER);
            case ASSIGN -> i -> i.assignTo(OTHER_TECH_ID, "MANUAL", LEAD, LATER);
            case ACKNOWLEDGE -> i -> i.acknowledge(TECH, LATER);
            case START_WORK -> i -> i.startWork(TECH, LATER);
            case REQUEST_INFO -> i -> i.requestInfo("Which room?", TECH, LATER);
            case PROVIDE_INFO -> i -> i.provideInfo("Room 101", REPORTER, LATER);
            case RESOLVE -> i -> i.resolve("Fixed it", null, TECH, LATER);
            case VERIFY -> i -> i.verify(REPORTER, LATER);
            case REOPEN -> i -> i.reopen("Still broken", REPORTER, LATER);
            case CLOSE -> i -> i.close(TECH, LATER);
            case MERGE -> i -> i.mergeInto(IncidentFixtures.newIncident(), LEAD, LATER);
            case FEEDBACK -> i -> i.submitFeedback(5, "Great", REPORTER, LATER);
            case ADD_NOTE, ESCALATE -> throw new IllegalArgumentException(action + " is not a transition");
        };
    }

    static Stream<Arguments> illegalPairs() {
        return Arrays.stream(IncidentStatus.values()).flatMap(s -> TRANSITIONS.stream()
                .filter(a -> !LEGAL.get(s).contains(a))
                .map(a -> Arguments.of(s, a)));
    }

    @ParameterizedTest(name = "{1} on {0} is rejected")
    @MethodSource("illegalPairs")
    void everyIllegalTransitionIsRejectedAndChangesNothing(IncidentStatus from, IncidentAction action) {
        Incident incident = inStatus(from);
        Long assigneeBefore = incident.getAssigneeId();

        assertThatThrownBy(() -> perform(action).accept(incident))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining(from.name());

        assertThat(incident.getStatus()).isEqualTo(from);
        assertThat(incident.getAssigneeId()).isEqualTo(assigneeBefore);
        assertThat(incident.hasPendingEvents()).as("no event for a rejected action").isFalse();
    }

    static Stream<Arguments> legalPairs() {
        return Stream.of(
                Arguments.of(NEW, CLASSIFY, CLASSIFIED),
                Arguments.of(NEW, MERGE, MERGED),
                Arguments.of(CLASSIFIED, ASSIGN, ASSIGNED),
                Arguments.of(CLASSIFIED, MERGE, MERGED),
                Arguments.of(ASSIGNED, ACKNOWLEDGE, ACKNOWLEDGED),
                Arguments.of(ASSIGNED, ASSIGN, ASSIGNED),
                Arguments.of(ACKNOWLEDGED, START_WORK, IN_PROGRESS),
                Arguments.of(ACKNOWLEDGED, ASSIGN, ASSIGNED),
                Arguments.of(IN_PROGRESS, REQUEST_INFO, PENDING_INFO),
                Arguments.of(IN_PROGRESS, RESOLVE, RESOLVED),
                Arguments.of(PENDING_INFO, PROVIDE_INFO, IN_PROGRESS),
                Arguments.of(RESOLVED, VERIFY, VERIFIED),
                Arguments.of(RESOLVED, REOPEN, IN_PROGRESS),
                Arguments.of(VERIFIED, CLOSE, CLOSED),
                Arguments.of(CLOSED, FEEDBACK, CLOSED));
    }

    @ParameterizedTest(name = "{1} on {0} → {2}")
    @MethodSource("legalPairs")
    void everyLegalTransitionReachesTheDocumentedStatus(IncidentStatus from, IncidentAction action,
            IncidentStatus to) {
        Incident incident = inStatus(from);

        perform(action).accept(incident);

        assertThat(incident.getStatus()).isEqualTo(to);
        assertThat(IncidentFixtures.pull(incident)).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("allStatuses")
    void legalPairsCoverTheWholeTable(IncidentStatus s) {
        assertThat(legalPairs().filter(a -> a.get()[0] == s).map(a -> (IncidentAction) a.get()[1]))
                .containsExactlyInAnyOrderElementsOf(LEGAL.get(s));
    }

    static Stream<IncidentStatus> allStatuses() {
        return Arrays.stream(IncidentStatus.values());
    }
}

package in.ac.bitspilani.triagegraph.sla.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

class SlaEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant PAST = NOW.minusSeconds(1);
    private static final Instant FUTURE = NOW.plusSeconds(1);

    private final SlaEvaluator evaluator = new SlaEvaluator();

    @ParameterizedTest
    @MethodSource("thresholdCases")
    void evaluatesThresholdBoundaries(
            SlaEvaluationInput input, List<SlaEvaluator.Decision> expected) {
        assertThat(evaluator.evaluate(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> thresholdCases() {
        return Stream.of(
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, FUTURE, FUTURE, FUTURE, false),
                        List.of(SlaEvaluator.Decision.RESPONSE_WARNING)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, NOW, FUTURE, FUTURE, FUTURE, false),
                        List.of(SlaEvaluator.Decision.RESPONSE_WARNING)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, FUTURE, FUTURE, FUTURE, FUTURE, false),
                        List.of()),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, NOW, NOW, FUTURE, FUTURE, false),
                        List.of(
                                SlaEvaluator.Decision.RESPONSE_WARNING,
                                SlaEvaluator.Decision.RESPONSE_BREACH)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, PAST, FUTURE, false),
                        List.of(SlaEvaluator.Decision.RESOLUTION_WARNING)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, NOW, FUTURE, false),
                        List.of(SlaEvaluator.Decision.RESOLUTION_WARNING)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, FUTURE, FUTURE, false),
                        List.of()),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, NOW, NOW, false),
                        List.of(
                                SlaEvaluator.Decision.RESOLUTION_WARNING,
                                SlaEvaluator.Decision.RESOLUTION_BREACH)));
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"CLOSED", "MERGED"})
    void terminalIncidentsProduceNoDecisions(IncidentStatus status) {
        assertThat(evaluator.evaluate(input(status, null, PAST, PAST, PAST, PAST, false)))
                .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"PENDING_INFO", "RESOLVED", "VERIFIED"})
    void pausedOrFinishedResolutionStatusesDoNotProduceResolutionDecisions(IncidentStatus status) {
        assertThat(evaluator.evaluate(input(status, NOW, FUTURE, FUTURE, PAST, PAST, false)))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("responseAcknowledgementCases")
    void acknowledgementStopsOnlyTheResponseClock(
            Instant respondedAt, List<SlaEvaluator.Decision> expected) {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.IN_PROGRESS, respondedAt, PAST, PAST, PAST, PAST, false)))
                .isEqualTo(expected);
    }

    private static Stream<Arguments> responseAcknowledgementCases() {
        return Stream.of(
                Arguments.of(null, List.of(
                        SlaEvaluator.Decision.RESPONSE_WARNING,
                        SlaEvaluator.Decision.RESOLUTION_WARNING,
                        SlaEvaluator.Decision.RESPONSE_BREACH,
                        SlaEvaluator.Decision.RESOLUTION_BREACH)),
                Arguments.of(NOW, List.of(
                        SlaEvaluator.Decision.RESOLUTION_WARNING,
                        SlaEvaluator.Decision.RESOLUTION_BREACH)));
    }

    @Test
    void pendingInfoPausesOnlyResolutionClock() {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.PENDING_INFO, null, PAST, PAST, PAST, PAST, false)))
                .containsExactly(
                        SlaEvaluator.Decision.RESPONSE_WARNING,
                        SlaEvaluator.Decision.RESPONSE_BREACH);
    }

    @Test
    void explicitResolutionPauseDoesNotPauseResponseClock() {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.IN_PROGRESS, null, PAST, PAST, PAST, PAST, true)))
                .containsExactly(
                        SlaEvaluator.Decision.RESPONSE_WARNING,
                        SlaEvaluator.Decision.RESPONSE_BREACH);
    }

    @Test
    void inactiveClocksProduceNoDecisions() {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.IN_PROGRESS, null, PAST, null, PAST, null, false)))
                .isEmpty();
    }

    @Test
    void aClockWithNoWarningThresholdCanStillBreach() {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.IN_PROGRESS, null, null, NOW, null, null, false)))
                .containsExactly(SlaEvaluator.Decision.RESPONSE_BREACH);
    }

    @Test
    void allWarningsPrecedeBreachesWhenBothClocksPassBothThresholds() {
        assertThat(evaluator.evaluate(input(
                IncidentStatus.IN_PROGRESS, null, PAST, PAST, PAST, PAST, false)))
                .containsExactly(
                        SlaEvaluator.Decision.RESPONSE_WARNING,
                        SlaEvaluator.Decision.RESOLUTION_WARNING,
                        SlaEvaluator.Decision.RESPONSE_BREACH,
                        SlaEvaluator.Decision.RESOLUTION_BREACH);
    }

    @Test
    void equalWarningAndBreachThresholdsAreAcceptedAndWarningsComeFirst() {
        SlaEvaluationInput input = input(
                IncidentStatus.IN_PROGRESS, null, NOW, NOW, NOW, NOW, false);

        assertThat(evaluator.evaluate(input))
                .containsExactly(
                        SlaEvaluator.Decision.RESPONSE_WARNING,
                        SlaEvaluator.Decision.RESOLUTION_WARNING,
                        SlaEvaluator.Decision.RESPONSE_BREACH,
                        SlaEvaluator.Decision.RESOLUTION_BREACH);
    }

    @ParameterizedTest
    @MethodSource("recordedDecisionCases")
    void doesNotReturnAlreadyRecordedDecisions(
            SlaEvaluationInput input, List<SlaEvaluator.Decision> expected) {
        assertThat(evaluator.evaluate(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> recordedDecisionCases() {
        return Stream.of(
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, PAST, FUTURE, FUTURE,
                        true, false, false, false, false),
                        List.of(SlaEvaluator.Decision.RESPONSE_BREACH)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, PAST, FUTURE, FUTURE,
                        false, true, false, false, false),
                        List.of()),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, PAST, FUTURE, FUTURE,
                        true, true, false, false, false),
                        List.of()),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, PAST, PAST,
                        false, false, true, false, false),
                        List.of(SlaEvaluator.Decision.RESOLUTION_BREACH)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, PAST, PAST,
                        false, false, false, true, false),
                        List.of()),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, NOW, FUTURE, FUTURE, PAST, PAST,
                        false, false, true, true, false),
                        List.of()));
    }

    @ParameterizedTest
    @MethodSource("independentRecordedFlagCases")
    void recordedFlagsForOneClockDoNotSuppressTheOtherClock(
            SlaEvaluationInput input, List<SlaEvaluator.Decision> expected) {
        assertThat(evaluator.evaluate(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> independentRecordedFlagCases() {
        return Stream.of(
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, PAST, PAST, PAST,
                        true, true, false, false, false),
                        List.of(
                                SlaEvaluator.Decision.RESOLUTION_WARNING,
                                SlaEvaluator.Decision.RESOLUTION_BREACH)),
                Arguments.of(input(IncidentStatus.IN_PROGRESS, null, PAST, PAST, PAST, PAST,
                        false, false, true, true, false),
                        List.of(
                                SlaEvaluator.Decision.RESPONSE_WARNING,
                                SlaEvaluator.Decision.RESPONSE_BREACH)));
    }

    @Test
    void rejectsWarningThresholdsLaterThanTheirDueTimes() {
        assertThatThrownBy(() -> input(
                IncidentStatus.IN_PROGRESS, null, FUTURE, NOW, FUTURE, FUTURE, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("responseWarningAt must not be later than responseDueAt");
        assertThatThrownBy(() -> input(
                IncidentStatus.IN_PROGRESS, null, FUTURE, FUTURE, FUTURE, NOW, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("resolutionWarningAt must not be later than resolutionDueAt");
    }

    @Test
    void requiresStatusAndEvaluationTime() {
        assertThatThrownBy(() -> new SlaEvaluationInput(
                null, null, null, null, false, false, null, null, false, false, false, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status");
        assertThatThrownBy(() -> new SlaEvaluationInput(
                IncidentStatus.IN_PROGRESS, null, null, null, false, false,
                null, null, false, false, false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("now");
    }

    @Test
    void rejectsNullEvaluatorInput() {
        assertThatThrownBy(() -> evaluator.evaluate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("input must not be null");
    }

    private static SlaEvaluationInput input(
            IncidentStatus status,
            Instant respondedAt,
            Instant responseWarningAt,
            Instant responseDueAt,
            Instant resolutionWarningAt,
            Instant resolutionDueAt,
            boolean resolutionPaused) {
        return input(status, respondedAt, responseWarningAt, responseDueAt,
                resolutionWarningAt, resolutionDueAt, false, false, false, false, resolutionPaused);
    }

    private static SlaEvaluationInput input(
            IncidentStatus status,
            Instant respondedAt,
            Instant responseWarningAt,
            Instant responseDueAt,
            Instant resolutionWarningAt,
            Instant resolutionDueAt,
            boolean responseWarningRecorded,
            boolean responseBreachRecorded,
            boolean resolutionWarningRecorded,
            boolean resolutionBreachRecorded,
            boolean resolutionPaused) {
        return new SlaEvaluationInput(
                status,
                respondedAt,
                responseWarningAt,
                responseDueAt,
                responseWarningRecorded,
                responseBreachRecorded,
                resolutionWarningAt,
                resolutionDueAt,
                resolutionWarningRecorded,
                resolutionBreachRecorded,
                resolutionPaused,
                NOW);
    }
}

package in.ac.bitspilani.triagegraph.priority.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

class WeightedScorePriorityCalculatorTest {

    private final WeightedScorePriorityCalculator calculator = new WeightedScorePriorityCalculator();

    private static PriorityInput input(Impact i, Urgency u, Criticality c, int users, int dependents) {
        return new PriorityInput(i, u, c, users, dependents, "APPLICATION");
    }

    @Test
    @DisplayName("TC02: I4, U4, C4, 300 users, 0 dependents → 83.2, P1, rationale lists every factor")
    void tc02FeePaymentOutage() {
        PriorityAssessment a = calculator.calculate(input(Impact.EXTENSIVE, Urgency.CRITICAL,
                Criticality.MISSION_CRITICAL, 300, 0));

        assertThat(a.score()).isEqualTo(83.2);
        assertThat(a.priority()).isEqualTo(Priority.P1);
        assertThat(a.calculatorKey()).isEqualTo("WEIGHTED_SCORE");
        assertThat(a.rationale()).contains(
                "Impact 4 (EXTENSIVE)", "Urgency 4 (CRITICAL)", "Criticality 4 (MISSION_CRITICAL)",
                "users 1.30 (300 affected)", "blast 1.00 (0 dependents)", "83.2", "P1");
    }

    /** Every row of the worked-example table in blueprint §7.1 must match exactly. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Fee payment app down,            EXTENSIVE,   CRITICAL, MISSION_CRITICAL, 300, 0, 83.2,  P1",
            "Campus SSO login failures,       EXTENSIVE,   CRITICAL, MISSION_CRITICAL, 500, 8, 103.2, P1",
            "Hostel Wi-Fi down in Block 5,    SIGNIFICANT, HIGH,     HIGH,             80,  0, 32.4,  P2",
            "ERP report export slow,          MODERATE,    MEDIUM,   HIGH,             15,  0, 13.2,  P3",
            "Lab printer jam,                 MINOR,       MEDIUM,   LOW,              3,   0, 2.0,   P4"
    })
    void workedExamplesFromBlueprint(String name, Impact i, Urgency u, Criticality c, int users, int deps,
            double expectedScore, Priority expectedPriority) {
        PriorityAssessment a = calculator.calculate(input(i, u, c, users, deps));
        assertThat(a.score()).isEqualTo(expectedScore);
        assertThat(a.priority()).isEqualTo(expectedPriority);
    }

    @ParameterizedTest(name = "TC07: score {0} → {1}")
    @CsvSource({ "40.0, P1", "39.9, P2", "20.0, P2", "19.9, P3", "8.0, P3", "7.9, P4" })
    void tc07ThresholdBoundaries(double score, Priority expected) {
        assertThat(WeightedScorePriorityCalculator.bandFor(score)).isEqualTo(expected);
    }

    @Test
    @DisplayName("TC07: a real input scoring exactly 40.0 is P1 (4×4×2 × 1.00 × 1.25)")
    void tc07ExactlyFortyEndToEnd() {
        PriorityAssessment a = calculator.calculate(input(Impact.EXTENSIVE, Urgency.CRITICAL,
                Criticality.MEDIUM, 5, 9));
        assertThat(a.score()).isEqualTo(40.0);
        assertThat(a.priority()).isEqualTo(Priority.P1);
    }

    @Test
    @DisplayName("TC07: just under 40 (4×3×3 × 1.10 = 39.6) is P2")
    void tc07JustUnderFortyEndToEnd() {
        PriorityAssessment a = calculator.calculate(input(Impact.EXTENSIVE, Urgency.HIGH,
                Criticality.HIGH, 10, 0));
        assertThat(a.score()).isEqualTo(39.6);
        assertThat(a.priority()).isEqualTo(Priority.P2);
    }

    @ParameterizedTest(name = "TC08: {0} users → ×{1}")
    @CsvSource({ "0, 1.00", "1, 1.00", "9, 1.00", "10, 1.10", "49, 1.10", "50, 1.20",
            "199, 1.20", "200, 1.30", "5000, 1.30" })
    void tc08AffectedUsersBoundaries(int users, double expected) {
        assertThat(WeightedScorePriorityCalculator.usersMultiplier(users)).isEqualTo(expected);
    }

    /** Base I×U×C = 2 × 1 × 3 = 6, so the score shows the multiplier directly. */
    @ParameterizedTest(name = "TC08 end to end: {0} users → score {1}")
    @CsvSource({ "9, 6.0", "10, 6.6", "49, 6.6", "50, 7.2", "199, 7.2", "200, 7.8" })
    void tc08MultiplierFlowsIntoScore(int users, double expectedScore) {
        PriorityAssessment a = calculator.calculate(input(Impact.MODERATE, Urgency.LOW,
                Criticality.HIGH, users, 0));
        assertThat(a.score()).isEqualTo(expectedScore);
    }

    @ParameterizedTest(name = "blast radius {0} dependents → ×{1}")
    @CsvSource({ "0, 1.00", "1, 1.03", "8, 1.24", "9, 1.25", "50, 1.25" })
    void blastRadiusIsCappedAtOnePointTwoFive(int dependents, double expected) {
        assertThat(WeightedScorePriorityCalculator.blastMultiplier(dependents)).isCloseTo(expected, within(1e-9));
    }

    @Test
    void rejectsMissingFactorsAndNegativeCounts() {
        assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> calculator.calculate(input(null, Urgency.LOW, Criticality.LOW, 1, 0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> calculator.calculate(input(Impact.MINOR, Urgency.LOW, Criticality.LOW, -1, 0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> calculator.calculate(input(Impact.MINOR, Urgency.LOW, Criticality.LOW, 1, -1)))
                .isInstanceOf(InvalidInputException.class);
    }
}

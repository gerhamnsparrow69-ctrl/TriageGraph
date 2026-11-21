package in.ac.bitspilani.triagegraph.priority.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.priority.domain.WeightedScorePriorityCalculator;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;

class PriorityServiceImplTest {

    private final PriorityServiceImpl service = new PriorityServiceImpl(
            new PriorityCalculatorRegistry(List.of(new WeightedScorePriorityCalculator())));

    @Test
    void usesTheActiveCalculator() {
        PriorityAssessment a = service.assess(new PriorityInput(Impact.EXTENSIVE, Urgency.CRITICAL,
                Criticality.MISSION_CRITICAL, 300, 0, "APPLICATION"));
        assertThat(a.calculatorKey()).isEqualTo("WEIGHTED_SCORE");
        assertThat(a.score()).isEqualTo(83.2);
        assertThat(a.priority()).isEqualTo(Priority.P1);
    }

    @Test
    void securityIncidentWithSignificantImpactIsAtLeastP2() {
        // 3 × 1 × 1 × 1.00 × 1.00 = 3.0 → P4 by score, raised to P2 by the guard
        PriorityAssessment a = service.assess(new PriorityInput(Impact.SIGNIFICANT, Urgency.LOW,
                Criticality.LOW, 1, 0, "SECURITY"));
        assertThat(a.score()).isEqualTo(3.0);
        assertThat(a.priority()).isEqualTo(Priority.P2);
        assertThat(a.rationale()).contains("raised to P2");
    }

    @Test
    void securityGuardNeedsSignificantImpact() {
        PriorityAssessment a = service.assess(new PriorityInput(Impact.MODERATE, Urgency.LOW,
                Criticality.LOW, 1, 0, "SECURITY"));
        assertThat(a.priority()).isEqualTo(Priority.P4);
    }

    @Test
    void securityGuardOnlyAppliesToSecurityCategory() {
        PriorityAssessment a = service.assess(new PriorityInput(Impact.SIGNIFICANT, Urgency.LOW,
                Criticality.LOW, 1, 0, "NETWORK"));
        assertThat(a.priority()).isEqualTo(Priority.P4);
    }

    @Test
    void securityGuardNeverLowersAPriority() {
        PriorityAssessment a = service.assess(new PriorityInput(Impact.EXTENSIVE, Urgency.CRITICAL,
                Criticality.MISSION_CRITICAL, 300, 0, "SECURITY"));
        assertThat(a.priority()).isEqualTo(Priority.P1);
        assertThat(a.rationale()).doesNotContain("raised");
    }
}

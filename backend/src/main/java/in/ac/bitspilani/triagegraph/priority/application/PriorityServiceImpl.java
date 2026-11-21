package in.ac.bitspilani.triagegraph.priority.application;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.PriorityService;

/**
 * Implements the shared {@link PriorityService} port: asks the active calculator for a score, then
 * applies the guard rules that hold whichever calculator is active (blueprint §7.1).
 * <ul>
 *   <li>A SECURITY incident with impact SIGNIFICANT or higher is at least P2.</li>
 * </ul>
 * "A Major Incident is always P1" is enforced by the MajorIncident aggregate, because
 * {@link PriorityInput} does not carry that flag.
 */
@Service
public class PriorityServiceImpl implements PriorityService {

    static final String SECURITY_CATEGORY = "SECURITY";

    private final PriorityCalculatorRegistry registry;

    public PriorityServiceImpl(PriorityCalculatorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public PriorityAssessment assess(PriorityInput in) {
        PriorityAssessment scored = registry.active().calculate(in);
        return applySecurityFloor(in, scored);
    }

    private static PriorityAssessment applySecurityFloor(PriorityInput in, PriorityAssessment scored) {
        boolean securityIncident = SECURITY_CATEGORY.equalsIgnoreCase(in.categoryCode());
        boolean significant = in.impact().weight() >= Impact.SIGNIFICANT.weight();
        if (securityIncident && significant && scored.priority().compareTo(Priority.P2) > 0) {
            return new PriorityAssessment(Priority.P2, scored.score(), scored.calculatorKey(),
                    scored.rationale() + "; raised to P2 (security incident with impact ≥ SIGNIFICANT)");
        }
        return scored;
    }
}

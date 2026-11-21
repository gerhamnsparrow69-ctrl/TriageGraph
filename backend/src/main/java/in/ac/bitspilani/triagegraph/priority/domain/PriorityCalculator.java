package in.ac.bitspilani.triagegraph.priority.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;

/**
 * Strategy for turning an incident's business impact into a priority (blueprint §7.1).
 * <p>
 * A calculator only scores. Business guard rules (security floor, major incident = P1) live in
 * {@code PriorityServiceImpl} so they apply to every calculator the admin can pick.
 */
public interface PriorityCalculator {

    /** Stable key stored in settings and returned in {@link PriorityAssessment#calculatorKey()}. */
    String key();

    /** Short human-readable description for the admin settings page. */
    String description();

    PriorityAssessment calculate(PriorityInput input);
}

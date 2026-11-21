package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;

/**
 * One way of choosing a technician (Strategy pattern). Every strategy receives only technicians that
 * already passed the {@link EligibilityFilter} (right skill, available, below max workload), so any
 * strategy can replace any other without the caller changing (Liskov substitution; see the shared
 * contract test suite).
 * <p>
 * New strategy = one new class annotated as a Spring bean. The {@link AssignmentStrategyRegistry}
 * picks it up and the admin can select it at runtime.
 */
public interface AssignmentStrategy {

    /** Stable key stored in settings, e.g. {@code SMART}. Upper case, no spaces. */
    String key();

    String displayName();

    /** One sentence for the admin settings page. */
    String description();

    /**
     * Ranks the eligible technicians, best first. Must return every eligible technician exactly once and
     * must not change any state (it is also used for previews and recommendations).
     */
    List<TechnicianRecommendation> rank(AssignmentContext context, List<TechnicianProfile> eligible);

    /** Called once after an automatic assignment really happened. Stateless strategies ignore it. */
    default void onAssigned(AssignmentContext context, long technicianId) {
    }
}

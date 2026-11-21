package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

/**
 * Runs before every strategy. A technician is eligible when they have the required skill, are marked
 * available, and are below their maximum workload. The two failure cases throw different exceptions so
 * the team lead knows whether to hire skills or rebalance work.
 */
public final class EligibilityFilter {

    /**
     * @throws TechnicianUnavailableException nobody has the skill, or everyone with it is unavailable
     * @throws WorkloadExceededException      everyone available with the skill is at max load
     */
    public List<TechnicianProfile> eligible(String requiredSkill, List<TechnicianProfile> technicians) {
        String skill = normalise(requiredSkill);
        List<TechnicianProfile> skilled = technicians.stream().filter(p -> hasSkill(p, skill)).toList();
        if (skilled.isEmpty()) {
            throw new TechnicianUnavailableException("No technician has the " + skill + " skill.");
        }
        List<TechnicianProfile> available = skilled.stream().filter(TechnicianProfile::available).toList();
        if (available.isEmpty()) {
            throw new TechnicianUnavailableException(
                    "All " + skilled.size() + " technicians with the " + skill + " skill are unavailable.");
        }
        List<TechnicianProfile> withCapacity = available.stream()
                .filter(p -> p.activeLoad() < p.maxLoad())
                .toList();
        if (withCapacity.isEmpty()) {
            throw new WorkloadExceededException("All " + available.size() + " available " + skill
                    + " technicians are at their maximum workload.");
        }
        return withCapacity;
    }

    public static boolean hasSkill(TechnicianProfile p, String skill) {
        return p.skills() != null && p.skills().stream()
                .filter(Objects::nonNull)
                .anyMatch(s -> s.trim().equalsIgnoreCase(skill));
    }

    public static String normalise(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("required skill must not be blank");
        }
        return skill.trim().toUpperCase(Locale.ROOT);
    }
}

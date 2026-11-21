package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;

/**
 * Intelligent Feature D (smart technician recommendation). A weighted score with readable reasons:
 * <pre>
 * smart = w1·skill + w2·headroom + w3·serviceExperience + w4·seniority
 *   skill             = 1 (every eligible technician has the required skill)
 *   headroom          = 1 − activeLoad / maxLoad
 *   serviceExperience = min(past fixes on this incident's service, cap) / cap
 *   seniority         = min(experience years, 10) / 10
 * normal weights (P3, P4): 0.40, 0.25, 0.20, 0.15
 * urgent weights (P1, P2): 0.35, 0.15, 0.20, 0.30   (prefer senior engineers when it matters most)
 * </pre>
 */
public class SmartScoreStrategy extends AbstractScoringStrategy {

    public static final String KEY = "SMART";

    /** Weights for skill, headroom, service experience and seniority; they must add up to 1. */
    public record Weights(double skill, double headroom, double experience, double seniority) {

        public Weights {
            double sum = skill + headroom + experience + seniority;
            if (Math.abs(sum - 1.0) > 1e-9 || skill < 0 || headroom < 0 || experience < 0 || seniority < 0) {
                throw new IllegalArgumentException("weights must be non-negative and add up to 1, got " + sum);
            }
        }
    }

    public static final Weights NORMAL = new Weights(0.40, 0.25, 0.20, 0.15);
    public static final Weights URGENT = new Weights(0.35, 0.15, 0.20, 0.30);
    public static final int MAX_SENIORITY_YEARS = 10;

    private final ServiceNameLookup serviceNames;
    private final int experienceCap;

    public SmartScoreStrategy(ServiceNameLookup serviceNames, int experienceCap) {
        if (experienceCap < 1) {
            throw new IllegalArgumentException("experience cap must be at least 1");
        }
        this.serviceNames = serviceNames;
        this.experienceCap = experienceCap;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Smart score";
    }

    @Override
    public String description() {
        return "Weighs skill, free capacity, past fixes on the same service and seniority; P1/P2 incidents favour senior engineers.";
    }

    @Override
    protected Score score(AssignmentContext context, TechnicianProfile t) {
        Weights w = context.urgent() ? URGENT : NORMAL;
        int fixes = fixesOn(t, context.request().serviceId());
        double experience = Math.min(fixes, experienceCap) / (double) experienceCap;
        double seniority = Math.min(t.experienceYears(), MAX_SENIORITY_YEARS) / (double) MAX_SENIORITY_YEARS;
        double value = w.skill() * 1.0 + w.headroom() * Workload.headroom(t)
                + w.experience() * experience + w.seniority() * seniority;

        List<String> reasons = new ArrayList<>();
        reasons.add(context.requiredSkill() + " skill");
        reasons.add(Workload.describe(t));
        if (fixes > 0) {
            reasons.add(fixes + (fixes == 1 ? " past fix on " : " past fixes on ")
                    + serviceNames.nameOf(context.request().serviceId()));
        }
        reasons.add(t.experienceYears() + (t.experienceYears() == 1 ? " yr experience" : " yrs experience"));
        if (context.urgent()) {
            reasons.add(context.request().priority() + ": seniority weighted higher");
        }
        return new Score(value, reasons);
    }

    static int fixesOn(TechnicianProfile t, long serviceId) {
        Map<Long, Integer> fixes = t.fixesByService();
        if (fixes == null) {
            return 0;
        }
        Integer n = fixes.get(serviceId);
        return n == null ? 0 : Math.max(0, n);
    }
}

package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;

/**
 * Prefers specialists: everyone eligible has the required skill, and among them the technician with the
 * fewest other skills ranks first (their depth in this area is likely higher). Ties go to the lower load.
 */
public class SkillBasedStrategy extends AbstractScoringStrategy {

    public static final String KEY = "SKILL_BASED";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Skill-based";
    }

    @Override
    public String description() {
        return "Picks a specialist in the incident's category: fewer other skills ranks higher, ties go to the lower workload.";
    }

    @Override
    protected Score score(AssignmentContext context, TechnicianProfile t) {
        int skillCount = Math.max(1, t.skills() == null ? 1 : t.skills().size());
        double specialisation = 1.0 / skillCount;                 // 1.0 for a single-skill specialist
        double value = 0.7 + 0.2 * specialisation + 0.1 * Workload.headroom(t);
        String depth = skillCount == 1 ? "specialist" : skillCount + " skills";
        return new Score(value, List.of(context.requiredSkill() + " skill (" + depth + ")", Workload.describe(t)));
    }
}

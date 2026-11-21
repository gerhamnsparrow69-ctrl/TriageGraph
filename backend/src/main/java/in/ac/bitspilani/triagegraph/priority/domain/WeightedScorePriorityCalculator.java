package in.ac.bitspilani.triagegraph.priority.domain;

import java.util.Locale;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/**
 * Default calculator (blueprint §7.1):
 * <pre>
 *   score = I × U × C × Musers × Mblast
 *   Musers: 1–9 → 1.00 · 10–49 → 1.10 · 50–199 → 1.20 · 200+ → 1.30
 *   Mblast: min(1 + 0.03 × D, 1.25), D = number of services that depend on this one
 *   P1 ≥ 40 · P2 ≥ 20 · P3 ≥ 8 · P4 &lt; 8
 * </pre>
 * The score is rounded to one decimal before banding, so the number shown to the user always
 * agrees with the priority it produced.
 */
@Component
public class WeightedScorePriorityCalculator implements PriorityCalculator {

    public static final String KEY = "WEIGHTED_SCORE";

    static final double P1_THRESHOLD = 40.0;
    static final double P2_THRESHOLD = 20.0;
    static final double P3_THRESHOLD = 8.0;

    static final double BLAST_STEP = 0.03;
    static final double BLAST_CAP = 1.25;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String description() {
        return "Impact × Urgency × Criticality × affected-users × blast-radius multipliers";
    }

    @Override
    public PriorityAssessment calculate(PriorityInput in) {
        validate(in);
        int i = in.impact().weight();
        int u = in.urgency().weight();
        int c = in.criticality().weight();
        double mUsers = usersMultiplier(in.affectedUsers());
        double mBlast = blastMultiplier(in.blastRadius());

        double score = round1(i * u * c * mUsers * mBlast);
        Priority priority = bandFor(score);

        String rationale = String.format(Locale.ROOT,
                "Impact %d (%s) × Urgency %d (%s) × Criticality %d (%s) × users %.2f (%d affected) "
                        + "× blast %.2f (%d dependents) = %.1f → %s",
                i, in.impact(), u, in.urgency(), c, in.criticality(),
                mUsers, in.affectedUsers(), mBlast, in.blastRadius(), score, priority);
        return new PriorityAssessment(priority, score, KEY, rationale);
    }

    static double usersMultiplier(int affectedUsers) {
        if (affectedUsers >= 200) return 1.30;
        if (affectedUsers >= 50) return 1.20;
        if (affectedUsers >= 10) return 1.10;
        return 1.00;
    }

    static double blastMultiplier(int dependents) {
        return Math.min(1.0 + BLAST_STEP * dependents, BLAST_CAP);
    }

    static Priority bandFor(double score) {
        if (score >= P1_THRESHOLD) return Priority.P1;
        if (score >= P2_THRESHOLD) return Priority.P2;
        if (score >= P3_THRESHOLD) return Priority.P3;
        return Priority.P4;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static void validate(PriorityInput in) {
        if (in == null || in.impact() == null || in.urgency() == null || in.criticality() == null) {
            throw new InvalidInputException("Impact, urgency and service criticality are required to calculate priority.");
        }
        if (in.affectedUsers() < 0) {
            throw new InvalidInputException("Affected users cannot be negative.");
        }
        if (in.blastRadius() < 0) {
            throw new InvalidInputException("Blast radius cannot be negative.");
        }
    }
}

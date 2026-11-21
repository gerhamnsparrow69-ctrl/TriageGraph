package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.Comparator;
import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;

/**
 * Template Method for strategies that give each technician an independent score. The skeleton is fixed
 * (score everyone, sort best first, break ties by lower load then lower id so results are deterministic);
 * subclasses only decide how one technician is scored and explained.
 */
public abstract class AbstractScoringStrategy implements AssignmentStrategy {

    /** A score in [0, 1] plus the human-readable reasons behind it. */
    protected record Score(double value, List<String> reasons) {
    }

    @Override
    public final List<TechnicianRecommendation> rank(AssignmentContext context, List<TechnicianProfile> eligible) {
        record Scored(TechnicianProfile profile, Score score) {
        }
        return eligible.stream()
                .map(p -> new Scored(p, score(context, p)))
                .sorted(Comparator.comparingDouble((Scored s) -> s.score().value()).reversed()
                        .thenComparingDouble(s -> Workload.ratio(s.profile()))
                        .thenComparingLong(s -> s.profile().id()))
                .map(s -> new TechnicianRecommendation(s.profile().id(), s.profile().name(),
                        Workload.round3(s.score().value()), key(), List.copyOf(s.score().reasons())))
                .toList();
    }

    /** Scores one eligible technician for this incident. */
    protected abstract Score score(AssignmentContext context, TechnicianProfile technician);
}

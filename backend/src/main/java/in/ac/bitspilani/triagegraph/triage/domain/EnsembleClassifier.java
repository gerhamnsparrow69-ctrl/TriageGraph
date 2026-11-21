package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Composite pattern: an {@link IncidentClassifier} made of other classifiers, used exactly like one of them.
 * <ol>
 *   <li>Every member votes; a member that fails is simply ignored.</li>
 *   <li>The most confident category wins, but a more trusted source (LLM &gt; kNN &gt; keywords) wins any vote
 *       within {@code tieMargin} of the leader.</li>
 *   <li>Each other member that agrees adds 0.10 confidence (capped at 0.99).</li>
 *   <li>The service comes from the most confident member that names one, preferring members that agree on
 *       the category.</li>
 * </ol>
 */
public class EnsembleClassifier implements IncidentClassifier {

    /** The chosen candidate plus every member's vote, so the UI can explain the decision. */
    public record Result(ClassificationCandidate chosen, List<ClassificationCandidate> votes, boolean lowConfidence) {
    }

    private static final Map<String, Integer> TRUST = Map.of(LlmClassifier.SOURCE, 3, KnnClassifier.SOURCE, 2,
            KeywordRuleClassifier.SOURCE, 1);
    static final double AGREEMENT_BONUS = 0.10;

    private final List<IncidentClassifier> members;
    private final double minConfidence;
    private final double tieMargin;
    private final String fallbackCategory;

    public EnsembleClassifier(List<IncidentClassifier> members, double minConfidence, double tieMargin,
            String fallbackCategory) {
        this.members = List.copyOf(members);
        this.minConfidence = minConfidence;
        this.tieMargin = tieMargin;
        this.fallbackCategory = fallbackCategory;
    }

    @Override
    public String source() {
        return "ENSEMBLE";
    }

    @Override
    public Optional<ClassificationCandidate> classify(TriageText input) {
        return Optional.of(classifyAll(input).chosen());
    }

    public Result classifyAll(TriageText input) {
        List<ClassificationCandidate> votes = new ArrayList<>();
        for (IncidentClassifier member : members) {
            try {
                member.classify(input).ifPresent(votes::add);
            } catch (RuntimeException e) {
                // a broken member must never break triage
            }
        }
        List<ClassificationCandidate> withCategory = votes.stream().filter(ClassificationCandidate::hasCategory)
                .sorted(Comparator.comparingDouble(ClassificationCandidate::confidence).reversed()).toList();
        if (withCategory.isEmpty()) {
            ClassificationCandidate service = bestService(votes, null);
            ClassificationCandidate fallback = new ClassificationCandidate(fallbackCategory, 0,
                    service == null ? null : service.serviceId(), service == null ? null : service.serviceName(),
                    service == null ? 0 : service.serviceConfidence(), "FALLBACK",
                    "No classifier was confident; please choose the category.");
            return new Result(fallback, votes, true);
        }
        ClassificationCandidate leader = withCategory.get(0);
        ClassificationCandidate best = leader;
        for (ClassificationCandidate c : withCategory) {
            if (c.confidence() >= leader.confidence() - tieMargin && trust(c) > trust(best)) {
                best = c;
            }
        }
        final ClassificationCandidate winner = best;
        String category = winner.categoryCode();
        List<ClassificationCandidate> agreeing = withCategory.stream().filter(c -> c.categoryCode().equals(category)).toList();
        double confidence = Math.min(0.99, winner.confidence() + AGREEMENT_BONUS * (agreeing.size() - 1));
        ClassificationCandidate service = bestService(votes, category);
        String explanation = winner.explanation();
        List<String> confirmers = agreeing.stream().filter(c -> c != winner).map(c -> label(c.source())).distinct().toList();
        if (!confirmers.isEmpty()) {
            explanation += " · confirmed by " + String.join(" and ", confirmers);
        }
        ClassificationCandidate chosen = new ClassificationCandidate(category, confidence,
                service == null ? null : service.serviceId(), service == null ? null : service.serviceName(),
                service == null ? 0 : service.serviceConfidence(), winner.source(), explanation);
        return new Result(chosen, votes, confidence < minConfidence);
    }

    private static ClassificationCandidate bestService(List<ClassificationCandidate> votes, String category) {
        return votes.stream().filter(ClassificationCandidate::hasService)
                .max(Comparator.comparingInt((ClassificationCandidate c) -> category != null && category.equals(c.categoryCode()) ? 1 : 0)
                        .thenComparingDouble(ClassificationCandidate::serviceConfidence)
                        .thenComparingInt(EnsembleClassifier::trust))
                .orElse(null);
    }

    static int trust(ClassificationCandidate c) {
        return TRUST.getOrDefault(c.source(), 0);
    }

    private static String label(String source) {
        return switch (source) {
            case LlmClassifier.SOURCE -> "Gemini";
            case KnnClassifier.SOURCE -> "similar incidents";
            case KeywordRuleClassifier.SOURCE -> "keywords";
            default -> source.toLowerCase(java.util.Locale.ROOT);
        };
    }
}

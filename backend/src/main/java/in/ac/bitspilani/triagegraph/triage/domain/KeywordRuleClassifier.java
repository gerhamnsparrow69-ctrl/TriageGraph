package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transparent rule-based classifier: counts admin-defined phrases. A multi-word phrase ("payment gateway")
 * counts double because it is more specific. Works with no history and no network, so it is always available.
 * <pre>confidence = min(0.90, 0.45 + 0.15 × min(score, 3)), minus 0.15 if two categories tie</pre>
 */
public class KeywordRuleClassifier implements IncidentClassifier {

    public static final String SOURCE = "KEYWORD";

    private final KeywordRules rules;
    private final ServiceCatalog catalog;

    public KeywordRuleClassifier(KeywordRules rules, ServiceCatalog catalog) {
        this.rules = rules;
        this.catalog = catalog;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public Optional<ClassificationCandidate> classify(TriageText input) {
        String text = TextNormalizer.forMatching(input.text());
        Map<String, Double> categoryScore = new LinkedHashMap<>();
        Map<String, Double> serviceScore = new LinkedHashMap<>();
        List<String> hits = new ArrayList<>();
        for (KeywordRule rule : rules.all()) {
            String phrase = TextNormalizer.forMatching(rule.phrase()).trim();
            if (phrase.isEmpty() || !text.contains(" " + phrase + " ")) {
                continue;
            }
            double weight = phrase.contains(" ") ? 2.0 : 1.0;
            Map<String, Double> target = rule.target() == KeywordRule.Target.CATEGORY ? categoryScore : serviceScore;
            target.merge(rule.key(), weight, Double::sum);
            if (!hits.contains(phrase)) {
                hits.add(phrase);
            }
        }
        if (categoryScore.isEmpty() && serviceScore.isEmpty()) {
            return Optional.empty();
        }
        Winner category = winner(categoryScore);
        Winner service = winner(serviceScore);
        Long serviceId = service == null ? null : catalog.byName(service.key()).map(ServiceCatalog.ServiceInfo::id).orElse(null);
        String explanation = "keywords: " + String.join(", ", hits.subList(0, Math.min(hits.size(), 5)));
        return Optional.of(new ClassificationCandidate(
                category == null ? null : category.key(), category == null ? 0 : category.confidence(),
                serviceId, service == null ? null : service.key(), service == null ? 0 : service.confidence(),
                SOURCE, explanation));
    }

    private record Winner(String key, double confidence) {
    }

    private static Winner winner(Map<String, Double> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Double>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())).toList();
        double best = sorted.get(0).getValue();
        double confidence = Math.min(0.90, 0.45 + 0.15 * Math.min(best, 3));
        if (sorted.size() > 1 && sorted.get(1).getValue().equals(best)) {
            confidence -= 0.15; // ambiguous: two targets matched equally well
        }
        return new Winner(sorted.get(0).getKey(), confidence);
    }
}

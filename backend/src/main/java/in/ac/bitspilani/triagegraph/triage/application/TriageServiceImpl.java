package in.ac.bitspilani.triagegraph.triage.application;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageResult;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.TriageService;
import in.ac.bitspilani.triagegraph.triage.domain.ClassificationCandidate;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.EnsembleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.RepeatSubmissionGuard;
import in.ac.bitspilani.triagegraph.triage.domain.ResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.ResolutionQuery;
import in.ac.bitspilani.triagegraph.triage.domain.ScoredView;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.TextNormalizer;
import in.ac.bitspilani.triagegraph.triage.domain.TriageText;
import in.ac.bitspilani.triagegraph.triage.domain.VectorIndex;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Facade over the intelligence layer and the implementation of the shared {@link TriageService} port.
 * The text is embedded once per request; the same vector feeds the classifiers and the duplicate search.
 */
@Service
public class TriageServiceImpl implements TriageService {

    /** Everything the report form shows: the suggestion, every classifier's vote and the duplicate bands. */
    public record Insight(ClassificationSuggestion suggestion, EnsembleClassifier.Result classification,
            DuplicateDetector.Report duplicates, String embedder) {
    }

    private final TextEmbedder embedder;
    private final EnsembleClassifier ensemble;
    private final DuplicateDetector detector;
    private final RepeatSubmissionGuard guard;
    private final ResolutionAdvisor advisor;
    private final VectorIndex index;
    private final KnowledgeBaseService knowledge;
    private final TriageSettingsService settings;
    private final ServiceCatalog catalog;
    private final TriageProperties properties;
    private final Clock clock;

    public TriageServiceImpl(TextEmbedder embedder, EnsembleClassifier ensemble, DuplicateDetector detector,
            RepeatSubmissionGuard guard, ResolutionAdvisor advisor, VectorIndex index, KnowledgeBaseService knowledge,
            TriageSettingsService settings, ServiceCatalog catalog, TriageProperties properties, Clock clock) {
        this.embedder = embedder;
        this.ensemble = ensemble;
        this.detector = detector;
        this.guard = guard;
        this.advisor = advisor;
        this.index = index;
        this.knowledge = knowledge;
        this.settings = settings;
        this.catalog = catalog;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public TriageResult preview(TriageRequest req) {
        Insight insight = analyse(req.title(), req.description(), null, null);
        return new TriageResult(insight.suggestion(),
                insight.duplicates().likely().stream().map(m -> m.view().toSimilar(m.score())).toList(),
                insight.duplicates().possibly().stream().map(m -> m.view().toSimilar(m.score())).toList());
    }

    /**
     * @param chosenServiceId the service the user picked, if any (otherwise the suggested one is used)
     * @param excludeIncidentId the incident being looked at, so it does not match itself
     */
    public Insight analyse(String title, String description, Long chosenServiceId, Long excludeIncidentId) {
        String text = TextNormalizer.combine(title, description);
        TriageText input = new TriageText(text, embedder.embed(text));
        EnsembleClassifier.Result result = ensemble.classifyAll(input);
        ClassificationSuggestion suggestion = toSuggestion(result);
        Long serviceId = chosenServiceId != null ? chosenServiceId : suggestion.serviceId();
        String serviceName = chosenServiceId != null
                ? catalog.byId(chosenServiceId).map(ServiceCatalog.ServiceInfo::name).orElse(null)
                : result.chosen().serviceName();
        DuplicateDetector.Report duplicates = text.length() < 3 ? DuplicateDetector.Report.empty()
                : detector.find(input.vector(), serviceId, serviceName, clock.instant(), settings.policy(), excludeIncidentId);
        return new Insight(suggestion, result, duplicates, embedder.name());
    }

    @Override
    public ClassificationSuggestion classify(String text) {
        String clean = TextNormalizer.clean(text);
        return toSuggestion(ensemble.classifyAll(new TriageText(clean, embedder.embed(clean))));
    }

    @Override
    public void index(long incidentId, String text) {
        knowledge.indexLive(incidentId, text);
    }

    @Override
    public void checkNotRepeatSubmission(long reporterId, String text) {
        guard.check(reporterId, embedder.embed(TextNormalizer.clean(text)), clock.instant(),
                properties.repeatWindow(), properties.repeatSimilarity());
    }

    @Override
    public List<SimilarIncident> similarResolved(long incidentId, int k) {
        KnowledgeBaseService.Indexed incident = knowledge.indexed(incidentId);
        return index.nearest(incident.vector(), Math.max(1, Math.min(k, 10)),
                        v -> v.hasResolution() && v.docId() != incident.view().docId())
                .stream().map(s -> s.view().toSimilar(s.similarity())).toList();
    }

    @Override
    public ResolutionSuggestion suggestResolution(long incidentId, boolean useAi) {
        KnowledgeBaseService.Indexed incident = knowledge.indexed(incidentId);
        return advisor.advise(new ResolutionQuery(incident.text(), incident.vector(), incident.view().docId(), 3, useAi));
    }

    /** Free-text search of past fixes (technician knowledge base page), with an optional AI draft. */
    public ResolutionSuggestion searchKnowledge(String query, boolean useAi) {
        String text = TextNormalizer.clean(query);
        return advisor.advise(new ResolutionQuery(text, embedder.embed(text), null, 5, useAi));
    }

    /** Resolved incidents most similar to free text, with similarity, for the knowledge-base page. */
    public List<ScoredView> similarResolvedToText(String query, int k) {
        String text = TextNormalizer.clean(query);
        return index.nearest(embedder.embed(text), Math.max(1, Math.min(k, 10)), v -> v.hasResolution());
    }

    ClassificationSuggestion toSuggestion(EnsembleClassifier.Result result) {
        ClassificationCandidate c = result.chosen();
        String explanation = Optional.ofNullable(c.explanation()).orElse("");
        if (result.lowConfidence() && !"FALLBACK".equals(c.source())) {
            explanation += " (low confidence: please confirm)";
        }
        String category = c.categoryCode();
        double confidence = c.confidence();
        List<String> active = catalog.categoryCodes();
        if (category != null && !active.isEmpty() && !active.contains(category)) {
            // The incident module only accepts active catalog categories, so never hand it one that was
            // retired or is missing (e.g. a category learnt from the archive): take the best active vote.
            Optional<ClassificationCandidate> best = result.votes().stream()
                    .filter(v -> v.hasCategory() && active.contains(v.categoryCode()))
                    .max(Comparator.comparingDouble(ClassificationCandidate::confidence));
            category = best.map(ClassificationCandidate::categoryCode).orElse(active.getFirst());
            confidence = best.map(ClassificationCandidate::confidence).orElse(0.0);
            explanation += " (" + c.categoryCode() + " is not an active category; please confirm)";
        }
        return new ClassificationSuggestion(category, c.serviceId(), Vectors.round3(confidence), c.source(),
                explanation);
    }
}

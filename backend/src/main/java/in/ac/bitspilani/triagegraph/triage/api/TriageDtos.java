package in.ac.bitspilani.triagegraph.triage.api;

import java.time.Instant;
import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.triage.application.TriageServiceImpl;
import in.ac.bitspilani.triagegraph.triage.domain.ClassificationCandidate;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRule;
import in.ac.bitspilani.triagegraph.triage.domain.ScoredView;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettings;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the triage endpoints. */
public final class TriageDtos {

    private TriageDtos() {
    }

    public record PreviewRequest(@Size(max = 200) String title, @Size(max = 4000) String description, Long serviceId) {
    }

    /** One classifier's vote, shown in the "why this suggestion?" panel. */
    public record Vote(String source, String categoryCode, double confidence, String serviceName,
            double serviceConfidence, String explanation) {

        static Vote from(ClassificationCandidate c) {
            return new Vote(c.source(), c.categoryCode(), Vectors.round3(c.confidence()), c.serviceName(),
                    Vectors.round3(c.serviceConfidence()), c.explanation());
        }
    }

    /** A possible duplicate with what the banner needs ("reported 4 min ago, 92% match"). */
    public record DuplicateView(long incidentId, String code, String title, IncidentStatus status, double score,
            double similarity, boolean sameService, long minutesAgo, String serviceName) {

        static DuplicateView from(DuplicateDetector.Match m) {
            return new DuplicateView(m.view().publicId(), m.view().code(), m.view().title(), m.view().status(),
                    m.score(), m.cosine(), m.sameService(), m.minutesAgo(), m.view().serviceName());
        }
    }

    public record PreviewResponse(ClassificationSuggestion suggestion, String suggestedServiceName, boolean lowConfidence,
            List<Vote> votes, List<DuplicateView> likelyDuplicates, List<DuplicateView> possiblyRelated, String embedder) {

        static PreviewResponse from(TriageServiceImpl.Insight i) {
            return new PreviewResponse(i.suggestion(), i.classification().chosen().serviceName(),
                    i.classification().lowConfidence(),
                    i.classification().votes().stream().map(Vote::from).toList(),
                    i.duplicates().likely().stream().map(DuplicateView::from).toList(),
                    i.duplicates().possibly().stream().map(DuplicateView::from).toList(), i.embedder());
        }
    }

    public record ServiceOption(Long id, String name) {
    }

    public record OptionsResponse(List<String> categories, List<ServiceOption> services) {
    }

    public record MergeRequest(@NotNull Long parentId) {
    }

    public record KnowledgeSearchRequest(@NotBlank @Size(max = 1000) String query, boolean useAi) {
    }

    public record KnowledgeHit(long id, String code, String title, double similarity, String categoryCode,
            String serviceName, String resolutionSummary, String rootCause) {

        static KnowledgeHit from(ScoredView s) {
            return new KnowledgeHit(s.view().publicId(), s.view().code(), s.view().title(),
                    Vectors.round3(s.similarity()), s.view().categoryCode(), s.view().serviceName(),
                    s.view().resolutionSummary(), s.view().rootCause());
        }
    }

    public record KnowledgeSearchResponse(ResolutionSuggestion suggestion, List<KnowledgeHit> hits) {
    }

    public record TriageSettingsView(double likelyThreshold, double possiblyThreshold, int windowHours,
            boolean aiEnabled, boolean llmConfigured, Instant updatedAt, Long updatedBy) {

        static TriageSettingsView from(TriageSettings s, boolean llmConfigured) {
            return new TriageSettingsView(s.getLikelyThreshold(), s.getPossiblyThreshold(), s.getWindowHours(),
                    s.isAiEnabled(), llmConfigured, s.getUpdatedAt(), s.getUpdatedBy());
        }
    }

    public record UpdateTriageSettingsRequest(
            @DecimalMin("0.05") @DecimalMax("0.99") double likelyThreshold,
            @DecimalMin("0.01") @DecimalMax("0.98") double possiblyThreshold,
            @Min(1) @Max(720) int windowHours,
            boolean aiEnabled) {
    }

    public record KeywordView(long id, KeywordRule.Target target, String key, String phrase) {

        static KeywordView from(KeywordRule r) {
            return new KeywordView(r.id() == null ? 0 : r.id(), r.target(), r.key(), r.phrase());
        }
    }

    public record AddKeywordRequest(@NotNull KeywordRule.Target target, @NotBlank @Size(max = 100) String key,
            @NotBlank @Size(max = 60) String phrase) {
    }
}

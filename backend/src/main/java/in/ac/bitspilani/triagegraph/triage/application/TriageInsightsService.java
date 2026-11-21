package in.ac.bitspilani.triagegraph.triage.application;

import java.util.List;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.triage.domain.DuplicatePolicy;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeSource;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeView;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.VectorIndex;

/** Status and evaluation numbers for the admin settings page (and for the report). */
@Service
public class TriageInsightsService {

    public record Status(String embedder, int indexSize, long archiveEntries, long liveEntries, long openLive,
            String llm, boolean llmConfigured, boolean aiEnabled, DuplicatePolicy policy) {
    }

    private final TextEmbedder embedder;
    private final VectorIndex index;
    private final LlmClient llm;
    private final TriageSettingsService settings;
    private volatile List<DuplicateEvaluation.LabelledPair> pairs;

    public TriageInsightsService(TextEmbedder embedder, VectorIndex index, LlmClient llm, TriageSettingsService settings) {
        this.embedder = embedder;
        this.index = index;
        this.llm = llm;
        this.settings = settings;
    }

    public Status status() {
        List<KnowledgeView> all = index.views(v -> true);
        long archive = all.stream().filter(v -> v.source() == KnowledgeSource.ARCHIVE).count();
        long live = all.size() - archive;
        long openLive = all.stream().filter(v -> v.source() == KnowledgeSource.LIVE && v.isOpen()).count();
        return new Status(embedder.name(), index.size(), archive, live, openLive, llm.status(), llm.enabled(),
                settings.aiEnabled(), settings.policy());
    }

    /** Precision, recall and F1 of duplicate detection on the labelled pairs at the current threshold. */
    public DuplicateEvaluation.Report evaluateDuplicates() {
        List<DuplicateEvaluation.LabelledPair> p = pairs;
        if (p == null) {
            p = DuplicateEvaluation.loadDefault();
            pairs = p;
        }
        return DuplicateEvaluation.evaluate(p, embedder, settings.policy().likelyThreshold());
    }
}

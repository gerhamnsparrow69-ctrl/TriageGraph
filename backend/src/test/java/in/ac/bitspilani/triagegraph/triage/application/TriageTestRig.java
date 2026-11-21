package in.ac.bitspilani.triagegraph.triage.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.triage.TriageFixtures;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.EnsembleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.HistoricalResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRuleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.KnnClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.LlmResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.RepeatSubmissionGuard;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.infrastructure.InMemoryVectorIndex;
import in.ac.bitspilani.triagegraph.triage.infrastructure.LexicalHashEmbedder;

/**
 * The whole triage module wired by hand, exactly as TriageConfig wires it, but with in-memory repositories,
 * a fake LLM and a fixed clock. Any embedder can be plugged in (lexical by default).
 */
final class TriageTestRig {

    static final Clock CLOCK = Clock.fixed(TriageFixtures.NOW, ZoneOffset.UTC);

    final InMemoryRepos.Knowledge knowledgeRepo = new InMemoryRepos.Knowledge();
    final InMemoryRepos.Keywords keywordRepo = new InMemoryRepos.Keywords();
    final InMemoryRepos.Settings settingsRepo = new InMemoryRepos.Settings();
    final InMemoryVectorIndex index = new InMemoryVectorIndex();
    final TriageFixtures.FakeLlm llm = new TriageFixtures.FakeLlm();
    final FakeIncidents incidents = new FakeIncidents();
    final TriageProperties properties = new TriageProperties(null, null, null, null, null, null, null, null, null, null);
    final ServiceCatalog catalog;
    final TextEmbedder embedder;
    final TriageSettingsService settings;
    final KeywordRuleService keywords;
    final KnowledgeBaseService knowledge;
    final TriageServiceImpl triage;

    TriageTestRig() {
        this(new LexicalHashEmbedder(), Map.of("Fee Payment App", 1L, "Hostel Wi-Fi", 6L, "LMS", 4L));
    }

    TriageTestRig(TextEmbedder embedder, Map<String, Long> serviceIds) {
        this.embedder = embedder;
        this.catalog = TriageFixtures.catalog(serviceIds);
        this.settings = new TriageSettingsService(settingsRepo, properties, CLOCK);
        this.keywords = new KeywordRuleService(keywordRepo, catalog);
        this.knowledge = new KnowledgeBaseService(knowledgeRepo, index, embedder, TriageFixtures.provider(incidents), catalog, CLOCK);
        var ensemble = new EnsembleClassifier(List.of(new KeywordRuleClassifier(keywords, catalog),
                new KnnClassifier(index, properties.knnNeighbours()), new LlmClassifier(llm, catalog, settings::aiEnabled)),
                properties.minConfidence(), 0.10, properties.fallbackCategory());
        var advisor = new LlmResolutionAdvisor(new HistoricalResolutionAdvisor(index, 0.30), llm, settings::aiEnabled);
        this.triage = new TriageServiceImpl(embedder, ensemble, new DuplicateDetector(index),
                new RepeatSubmissionGuard(index), advisor, index, knowledge, settings, catalog, properties, CLOCK);
    }

    void archive(String code, String title, String description, String category, String service, String resolution) {
        knowledge.addArchived(new KnowledgeBaseService.ArchivedIncident(code, title, description, category, service,
                resolution, "root cause of " + code, 4L, Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z")));
    }

    static final class FakeIncidents implements IncidentQueryPort {
        final List<IncidentSnapshot> all = new ArrayList<>();

        @Override
        public Optional<IncidentSnapshot> find(long id) {
            return all.stream().filter(s -> s.id() == id).findFirst();
        }

        @Override
        public List<IncidentSnapshot> openSince(Instant since) {
            return List.copyOf(all);
        }

        @Override
        public List<IncidentSnapshot> openWithDeadlineBefore(Instant t) {
            return List.of();
        }

        void replace(IncidentSnapshot s) {
            all.removeIf(x -> x.id() == s.id());
            all.add(s);
        }
    }
}

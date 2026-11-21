package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.ac.bitspilani.triagegraph.triage.application.TriageProperties;
import in.ac.bitspilani.triagegraph.triage.application.TriageSettingsService;
import in.ac.bitspilani.triagegraph.triage.domain.DuplicateDetector;
import in.ac.bitspilani.triagegraph.triage.domain.EnsembleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.HistoricalResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRuleClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRules;
import in.ac.bitspilani.triagegraph.triage.domain.KnnClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClassifier;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.RepeatSubmissionGuard;
import in.ac.bitspilani.triagegraph.triage.domain.ResolutionAdvisor;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.VectorIndex;

/**
 * Composition root of the triage module: the only place that knows which adapter and decorator sits behind
 * each domain interface (Dependency Inversion). The domain classes themselves are plain Java.
 */
@Configuration
public class TriageConfig {

    private static final Logger log = LoggerFactory.getLogger(TriageConfig.class);
    static final double MIN_RESOLUTION_SIMILARITY = 0.30;
    static final double ENSEMBLE_TIE_MARGIN = 0.10;

    @Bean
    TextEmbedder textEmbedder(TriageProperties props) {
        LexicalHashEmbedder lexical = new LexicalHashEmbedder();
        if ("lexical".equalsIgnoreCase(props.embedder())) {
            return lexical;
        }
        return new FallbackTextEmbedder(MiniLmTextEmbedder::new, lexical);
    }

    @Bean
    VectorIndex vectorIndex() {
        return new InMemoryVectorIndex();
    }

    /** Cache → circuit breaker → Gemini, or the Null Object when there is no key. */
    @Bean
    LlmClient llmClient(TriageProperties props, Clock clock) {
        TriageProperties.Llm llm = props.llm();
        if (!llm.enabled()) {
            return new NoOpLlmClient("switched off in configuration (app.triage.llm.enabled=false)");
        }
        if (!llm.configured()) {
            log.info("No GEMINI_API_KEY set: AI classification and drafting are off; local triage still works.");
            return new NoOpLlmClient("no GEMINI_API_KEY set");
        }
        LlmClient gemini = new GeminiLlmClient(llm.baseUrl(), llm.model(), llm.apiKey(), llm.timeout());
        return new CachingLlmClient(new CircuitBreakerLlmClient(gemini, llm.failureThreshold(), llm.openDuration(), clock),
                llm.cacheSize());
    }

    @Bean
    KeywordRuleClassifier keywordRuleClassifier(KeywordRules rules, ServiceCatalog catalog) {
        return new KeywordRuleClassifier(rules, catalog);
    }

    @Bean
    KnnClassifier knnClassifier(VectorIndex index, TriageProperties props) {
        return new KnnClassifier(index, props.knnNeighbours());
    }

    @Bean
    LlmClassifier llmClassifier(LlmClient llm, ServiceCatalog catalog, TriageSettingsService settings) {
        return new LlmClassifier(llm, catalog, settings::aiEnabled);
    }

    @Bean
    EnsembleClassifier ensembleClassifier(KeywordRuleClassifier keywords, KnnClassifier knn, LlmClassifier llm,
            TriageProperties props) {
        return new EnsembleClassifier(List.of(keywords, knn, llm), props.minConfidence(), ENSEMBLE_TIE_MARGIN,
                props.fallbackCategory());
    }

    @Bean
    DuplicateDetector duplicateDetector(VectorIndex index) {
        return new DuplicateDetector(index);
    }

    @Bean
    RepeatSubmissionGuard repeatSubmissionGuard(VectorIndex index) {
        return new RepeatSubmissionGuard(index);
    }

    /** Historical advisor wrapped by the LLM decorator (exposed as one bean to avoid ambiguity). */
    @Bean
    ResolutionAdvisor resolutionAdvisor(VectorIndex index, LlmClient llm, TriageSettingsService settings) {
        return new LlmResolutionAdvisor(new HistoricalResolutionAdvisor(index, MIN_RESOLUTION_SIMILARITY), llm,
                settings::aiEnabled);
    }
}

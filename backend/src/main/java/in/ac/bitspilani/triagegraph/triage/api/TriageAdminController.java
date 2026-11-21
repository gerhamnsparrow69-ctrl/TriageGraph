package in.ac.bitspilani.triagegraph.triage.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.AddKeywordRequest;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.KeywordView;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.TriageSettingsView;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.UpdateTriageSettingsRequest;
import in.ac.bitspilani.triagegraph.triage.application.DuplicateEvaluation;
import in.ac.bitspilani.triagegraph.triage.application.KeywordRuleService;
import in.ac.bitspilani.triagegraph.triage.application.TriageInsightsService;
import in.ac.bitspilani.triagegraph.triage.application.TriageSettingsService;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Admin tuning of triage: thresholds, the AI switch, keyword rules, status and the measured accuracy. */
@RestController
@RequestMapping("/api/v1/admin/triage")
@Tag(name = "Admin: triage")
public class TriageAdminController {

    private final TriageSettingsService settings;
    private final KeywordRuleService keywords;
    private final TriageInsightsService insights;
    private final LlmClient llm;

    public TriageAdminController(TriageSettingsService settings, KeywordRuleService keywords,
            TriageInsightsService insights, LlmClient llm) {
        this.settings = settings;
        this.keywords = keywords;
        this.insights = insights;
        this.llm = llm;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    public TriageSettingsView settings() {
        return TriageSettingsView.from(settings.current(), llm.enabled());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change duplicate thresholds, the search window and the AI switch")
    public TriageSettingsView update(@Valid @RequestBody UpdateTriageSettingsRequest body, @AuthenticationPrincipal Jwt jwt) {
        return TriageSettingsView.from(settings.update(body.likelyThreshold(), body.possiblyThreshold(),
                body.windowHours(), body.aiEnabled(), Callers.id(jwt)), llm.enabled());
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Embedder in use, index size and LLM health")
    public TriageInsightsService.Status status() {
        return insights.status();
    }

    @GetMapping("/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Precision, recall and F1 of duplicate detection on the labelled pairs")
    public DuplicateEvaluation.Report evaluation() {
        return insights.evaluateDuplicates();
    }

    @GetMapping("/keywords")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    public List<KeywordView> keywords() {
        return keywords.list().stream().map(KeywordView::from).toList();
    }

    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a keyword or alias; it is used by the very next preview")
    public KeywordView addKeyword(@Valid @RequestBody AddKeywordRequest body) {
        return KeywordView.from(keywords.add(body.target(), body.key(), body.phrase()));
    }

    @DeleteMapping("/keywords/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeKeyword(@PathVariable long id) {
        keywords.remove(id);
    }
}

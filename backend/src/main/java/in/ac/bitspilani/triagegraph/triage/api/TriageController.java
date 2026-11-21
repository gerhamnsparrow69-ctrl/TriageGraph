package in.ac.bitspilani.triagegraph.triage.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.KnowledgeHit;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.KnowledgeSearchRequest;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.KnowledgeSearchResponse;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.MergeRequest;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.OptionsResponse;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.PreviewRequest;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.PreviewResponse;
import in.ac.bitspilani.triagegraph.triage.api.TriageDtos.ServiceOption;
import in.ac.bitspilani.triagegraph.triage.application.JoinMergeService;
import in.ac.bitspilani.triagegraph.triage.application.TriageServiceImpl;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Live triage for the report form, duplicate join/merge, and resolution help for technicians. Everything is
 * under {@code /api/v1/triage} so it can never clash with another module's mappings.
 */
@RestController
@RequestMapping("/api/v1/triage")
@Tag(name = "Triage")
public class TriageController {

    private final TriageServiceImpl triage;
    private final JoinMergeService joinMerge;
    private final ServiceCatalog catalog;

    public TriageController(TriageServiceImpl triage, JoinMergeService joinMerge, ServiceCatalog catalog) {
        this.triage = triage;
        this.joinMerge = joinMerge;
        this.catalog = catalog;
    }

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Suggest category and service, and find likely duplicates, while the user types")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest body) {
        return PreviewResponse.from(triage.analyse(body.title(), body.description(), body.serviceId(), null));
    }

    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Categories and services the report form can offer")
    public OptionsResponse options() {
        return new OptionsResponse(catalog.categoryCodes(),
                catalog.services().stream().map(s -> new ServiceOption(s.id(), s.name())).toList());
    }

    @GetMapping("/incidents/{incidentId}/similar")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Most similar resolved incidents, with how they were fixed")
    public List<SimilarIncident> similar(@PathVariable long incidentId, @RequestParam(defaultValue = "3") int k) {
        return triage.similarResolved(incidentId, k);
    }

    @PostMapping("/incidents/{incidentId}/resolution-suggestion")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Draft resolution from similar past fixes (AI draft when Gemini is configured)")
    public ResolutionSuggestion resolutionSuggestion(@PathVariable long incidentId,
            @RequestParam(defaultValue = "true") boolean useAi) {
        return triage.suggestResolution(incidentId, useAi);
    }

    @PostMapping("/incidents/{incidentId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TECHNICIAN', 'TEAM_LEAD')")
    @Operation(summary = "I have the same problem: follow this incident instead of reporting a new one")
    public void join(@PathVariable long incidentId, @AuthenticationPrincipal Jwt jwt) {
        joinMerge.join(incidentId, Callers.id(jwt));
    }

    @PostMapping("/incidents/{incidentId}/merge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN')")
    @Operation(summary = "Merge this incident into its parent as a duplicate")
    public void merge(@PathVariable long incidentId, @Valid @RequestBody MergeRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        joinMerge.merge(incidentId, body.parentId(), Callers.id(jwt));
    }

    @PostMapping("/knowledge/search")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Search past fixes by describing a problem")
    public KnowledgeSearchResponse searchKnowledge(@Valid @RequestBody KnowledgeSearchRequest body) {
        List<KnowledgeHit> hits = triage.similarResolvedToText(body.query(), 5).stream().map(KnowledgeHit::from).toList();
        return new KnowledgeSearchResponse(triage.searchKnowledge(body.query(), body.useAi()), hits);
    }
}

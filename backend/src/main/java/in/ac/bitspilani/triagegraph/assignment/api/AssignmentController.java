package in.ac.bitspilani.triagegraph.assignment.api;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
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

import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.ReassignRequest;
import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.RecommendationQuery;
import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.StrategyView;
import in.ac.bitspilani.triagegraph.assignment.application.AssignmentDependencyUnavailableException;
import in.ac.bitspilani.triagegraph.assignment.application.AssignmentServiceImpl;
import in.ac.bitspilani.triagegraph.assignment.application.AssignmentServiceImpl.StrategyOutcome;
import in.ac.bitspilani.triagegraph.assignment.application.AssignmentSettingsService;
import in.ac.bitspilani.triagegraph.assignment.application.ReassignmentService;
import in.ac.bitspilani.triagegraph.assignment.application.WorkloadService;
import in.ac.bitspilani.triagegraph.assignment.application.WorkloadService.TechnicianWorkload;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Technician recommendations, strategy comparison, workload and manual reassignment. Everything lives
 * under {@code /api/v1/assignment} so it can never clash with another module's mappings.
 */
@RestController
@RequestMapping("/api/v1/assignment")
@Tag(name = "Assignment")
public class AssignmentController {

    private final AssignmentServiceImpl assignment;
    private final AssignmentSettingsService settings;
    private final WorkloadService workload;
    private final ReassignmentService reassignment;
    private final UserDirectory directory;
    private final ObjectProvider<IncidentQueryPort> incidents;
    private final AssignmentStrategyRegistry registry;

    public AssignmentController(AssignmentServiceImpl assignment, AssignmentSettingsService settings,
            WorkloadService workload, ReassignmentService reassignment, UserDirectory directory,
            ObjectProvider<IncidentQueryPort> incidents, AssignmentStrategyRegistry registry) {
        this.registry = registry;
        this.assignment = assignment;
        this.settings = settings;
        this.workload = workload;
        this.reassignment = reassignment;
        this.directory = directory;
        this.incidents = incidents;
    }

    @GetMapping("/strategies")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "All assignment strategies and which one is active")
    public List<StrategyView> strategies() {
        return AssignmentAdminController.views(registry, settings.current().getActiveStrategy());
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Rank technicians for an incident like this (active strategy unless one is named)")
    public List<TechnicianRecommendation> recommend(@Valid @RequestBody RecommendationQuery query) {
        AssignmentRequest req = query.toRequest();
        return query.strategyKey() == null || query.strategyKey().isBlank()
                ? assignment.recommend(req, query.limit())
                : assignment.recommendWith(query.strategyKey(), req, query.limit());
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "What every strategy would choose for the same incident")
    public List<StrategyOutcome> compare(@Valid @RequestBody RecommendationQuery query) {
        return assignment.compare(query.toRequest());
    }

    @GetMapping("/incidents/{incidentId}/recommendations")
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Top technicians for an existing incident")
    public List<TechnicianRecommendation> forIncident(@PathVariable long incidentId,
            @RequestParam(defaultValue = "3") int topK) {
        IncidentQueryPort port = incidents.getIfAvailable();
        if (port == null) {
            throw new AssignmentDependencyUnavailableException("Incident data is not available yet.");
        }
        IncidentSnapshot s = port.find(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("No incident with id " + incidentId));
        if (s.categoryCode() == null || s.priority() == null) {
            throw new InvalidInputException(s.code() + " has not been classified and prioritised yet.");
        }
        return assignment.recommend(new AssignmentRequest(s.id(), s.categoryCode(), s.serviceId(), s.priority()),
                Math.min(Math.max(topK, 1), 10));
    }

    @PostMapping("/incidents/{incidentId}/reassign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'ADMIN')")
    @Operation(summary = "Reassign an incident (capacity rules apply unless an admin overrides)")
    public void reassign(@PathVariable long incidentId, @Valid @RequestBody ReassignRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        long actor = Callers.id(jwt);
        if (body.override() && directory.user(actor).role() != Role.ADMIN) {
            throw new UnauthorizedOperationException("Only an administrator can override capacity rules.");
        }
        reassignment.reassign(incidentId, body.technicianId(), body.reason(), actor, body.override());
    }

    @GetMapping("/workload/me")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD')")
    @Operation(summary = "My workload and queue, most urgent first")
    public TechnicianWorkload myWorkload(@AuthenticationPrincipal Jwt jwt) {
        return workload.forTechnician(Callers.id(jwt));
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Workload of my team (team lead) or of everyone (admin, IT manager)")
    public List<TechnicianWorkload> workload(@AuthenticationPrincipal Jwt jwt) {
        long caller = Callers.id(jwt);
        return directory.user(caller).role() == Role.TEAM_LEAD ? workload.team(caller) : workload.everyone();
    }
}

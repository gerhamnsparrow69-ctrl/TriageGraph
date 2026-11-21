package in.ac.bitspilani.triagegraph.incident.api;

import in.ac.bitspilani.triagegraph.incident.application.IncidentLifecycleService;
import in.ac.bitspilani.triagegraph.incident.application.IncidentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The incident lifecycle over HTTP. Every response includes {@code allowedActions}; an action that the
 * current state does not allow returns 409 INVALID_STATE_TRANSITION, one the caller may not do returns 403.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Report an incident and move it through its lifecycle")
public class IncidentController {

    private final IncidentLifecycleService lifecycle;

    public IncidentController(IncidentLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @PostMapping
    @Operation(summary = "Report an incident: classified, prioritised and auto-assigned in one step")
    public ResponseEntity<IncidentView> report(@Valid @RequestBody IncidentRequests.Report body,
            @AuthenticationPrincipal Jwt jwt) {
        IncidentView view = lifecycle.report(body.toCommand(), JwtActors.from(jwt));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(view.id()).toUri()).body(view);
    }

    @GetMapping("/{id}")
    public IncidentView get(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.get(id, JwtActors.from(jwt));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign or reassign (team lead, admin)")
    public IncidentView assign(@PathVariable long id, @Valid @RequestBody IncidentRequests.Assign body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.assign(id, body.technicianId(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Assignee accepts the incident (stops the response SLA)")
    public IncidentView acknowledge(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.acknowledge(id, JwtActors.from(jwt));
    }

    @PostMapping("/{id}/start")
    public IncidentView startWork(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.startWork(id, JwtActors.from(jwt));
    }

    @PostMapping("/{id}/request-info")
    @Operation(summary = "Ask the reporter a question (pauses the SLA)")
    public IncidentView requestInfo(@PathVariable long id, @Valid @RequestBody IncidentRequests.Message body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.requestInfo(id, body.text(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/provide-info")
    @Operation(summary = "Reporter answers (resumes the SLA)")
    public IncidentView provideInfo(@PathVariable long id, @Valid @RequestBody IncidentRequests.Message body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.provideInfo(id, body.text(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/resolve")
    public IncidentView resolve(@PathVariable long id, @Valid @RequestBody IncidentRequests.Resolve body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.resolve(id, body.summary(), body.rootCause(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Reporter confirms the fix")
    public IncidentView verify(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.verify(id, JwtActors.from(jwt));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reporter rejects the fix, with a reason")
    public IncidentView reopen(@PathVariable long id, @Valid @RequestBody IncidentRequests.Message body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.reopen(id, body.text(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/close")
    public IncidentView close(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.close(id, JwtActors.from(jwt));
    }

    @PostMapping("/{id}/notes")
    public IncidentView addNote(@PathVariable long id, @Valid @RequestBody IncidentRequests.Note body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.addNote(id, body.text(), body.isInternal(), JwtActors.from(jwt));
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Reporter rates a closed incident (once)")
    public IncidentView feedback(@PathVariable long id, @Valid @RequestBody IncidentRequests.Feedback body,
            @AuthenticationPrincipal Jwt jwt) {
        return lifecycle.submitFeedback(id, body.rating(), body.comment(), JwtActors.from(jwt));
    }
}

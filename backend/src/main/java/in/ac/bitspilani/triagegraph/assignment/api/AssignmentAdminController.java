package in.ac.bitspilani.triagegraph.assignment.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.AssignmentSettingsView;
import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.StrategyView;
import in.ac.bitspilani.triagegraph.assignment.api.AssignmentDtos.SwitchStrategyRequest;
import in.ac.bitspilani.triagegraph.assignment.application.AssignmentSettingsService;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettings;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Admin configuration of the assignment strategy (Strategy pattern, switched live). */
@RestController
@RequestMapping("/api/v1/admin/assignment")
@Tag(name = "Admin: assignment")
public class AssignmentAdminController {

    private final AssignmentSettingsService settings;
    private final AssignmentStrategyRegistry registry;

    public AssignmentAdminController(AssignmentSettingsService settings, AssignmentStrategyRegistry registry) {
        this.settings = settings;
        this.registry = registry;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Active assignment strategy and the available ones")
    public AssignmentSettingsView current() {
        return view(settings.current());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Switch the assignment strategy; the next assignment uses it, no restart")
    public AssignmentSettingsView switchStrategy(@Valid @RequestBody SwitchStrategyRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        return view(settings.switchTo(body.strategyKey(), Callers.id(jwt)));
    }

    private AssignmentSettingsView view(AssignmentSettings s) {
        return new AssignmentSettingsView(s.getActiveStrategy(), views(registry, s.getActiveStrategy()),
                s.getUpdatedAt(), s.getUpdatedBy());
    }

    static List<StrategyView> views(AssignmentStrategyRegistry registry, String activeKey) {
        return registry.all().stream()
                .map(st -> new StrategyView(st.key(), st.displayName(), st.description(),
                        st.key().equalsIgnoreCase(activeKey)))
                .toList();
    }
}

package in.ac.bitspilani.triagegraph.audit.api;

import in.ac.bitspilani.triagegraph.audit.application.AuditEntryView;
import in.ac.bitspilani.triagegraph.audit.application.AuditService;
import in.ac.bitspilani.triagegraph.audit.application.AuditVerification;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Audit", description = "Tamper-evident, hash-chained history of every change")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/api/v1/incidents/{id}/timeline")
    @Operation(summary = "Every recorded step of one incident, oldest first")
    public List<AuditEntryView> timeline(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (jwt.getSubject() == null || roles == null || roles.isEmpty()) {
            throw new UnauthorizedOperationException("The token does not identify a user.");
        }
        return audit.timeline(id, Long.parseLong(jwt.getSubject()), Role.valueOf(roles.getFirst()));
    }

    @GetMapping("/api/v1/admin/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    @Operation(summary = "The whole audit trail, newest first")
    public List<AuditEntryView> recent(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return audit.recent(page, size);
    }

    @GetMapping("/api/v1/admin/audit/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    @Operation(summary = "Recompute the SHA-256 chain and report the first tampered entry, if any")
    public AuditVerification verify() {
        return audit.verify();
    }
}

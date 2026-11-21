package in.ac.bitspilani.triagegraph.sla.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/sla")
public class SlaController {

    private static final String SLA_READ_ROLES =
            "hasAnyRole('EMPLOYEE', 'TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')";

    private final SlaService sla;

    public SlaController(SlaService sla) {
        this.sla = sla;
    }

    @GetMapping("/targets")
    @PreAuthorize(SLA_READ_ROLES)
    public SlaTargets targets(@RequestParam @Positive long serviceId,
            @RequestParam Priority priority) {
        return sla.targetsFor(serviceId, priority);
    }
}

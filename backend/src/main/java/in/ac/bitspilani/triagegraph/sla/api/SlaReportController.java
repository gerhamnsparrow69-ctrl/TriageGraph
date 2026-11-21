package in.ac.bitspilani.triagegraph.sla.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.sla.application.SlaBreachReport;
import in.ac.bitspilani.triagegraph.sla.application.SlaReportingService;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class SlaReportController {

    private final SlaReportingService reports;

    public SlaReportController(SlaReportingService reports) {
        this.reports = reports;
    }

    @GetMapping("/sla-breaches")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    public SlaBreachReport slaBreaches() {
        return reports.breachCounts();
    }
}

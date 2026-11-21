package in.ac.bitspilani.triagegraph.reporting.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.reporting.application.Reports;
import in.ac.bitspilani.triagegraph.reporting.application.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
@PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER', 'TEAM_LEAD')")
public class ReportController {

    private final ReportingService reporting;

    public ReportController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @Operation(summary = "All six reports: overview, technician workload, service analysis, 14-day trend, "
            + "SLA compliance by priority and AI effectiveness")
    @GetMapping
    public Reports.Dashboard dashboard() {
        return reporting.dashboard();
    }

    @Operation(summary = "Just the KPI tiles (total, open, critical, resolved, closed, SLA violations, MTTA, MTTR)")
    @GetMapping("/overview")
    public Reports.Overview overview() {
        return reporting.overview();
    }
}

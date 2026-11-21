package in.ac.bitspilani.triagegraph.reporting.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.reporting.application.IncidentSearchCriteria;
import in.ac.bitspilani.triagegraph.reporting.application.IncidentSearchService;
import in.ac.bitspilani.triagegraph.reporting.application.IncidentSort;
import in.ac.bitspilani.triagegraph.reporting.application.ReportExporter;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Incident search for every role; results are scoped to what the caller may see. */
@RestController
@RequestMapping("/api/v1/search/incidents")
@Tag(name = "Search")
public class SearchController {

    private final IncidentSearchService search;
    private final ReportExporter exporter;
    private final Clock clock;

    public SearchController(IncidentSearchService search, ReportExporter exporter, Clock clock) {
        this.search = search;
        this.exporter = exporter;
        this.clock = clock;
    }

    @Operation(summary = "Search incidents by up to 11 criteria (all optional, combined with AND)",
            description = "sort: newest | oldest | priority | due-soonest | status. size: 1 to 100.")
    @GetMapping
    public IncidentSearchService.SearchPage search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<IncidentStatus> status,
            @RequestParam(required = false) Set<Priority> priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Boolean major,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        IncidentSearchCriteria criteria = new IncidentSearchCriteria(code, from, to, status, priority, category,
                serviceId, technicianId, slaStatus, reporterId, major, q);
        return search.search(criteria, userId(jwt), role(jwt), IncidentSort.parse(sort), page, size);
    }

    @Operation(summary = "Values for the service, category and technician filters")
    @GetMapping("/options")
    public IncidentSearchService.SearchOptions options() {
        return search.options();
    }

    @Operation(summary = "Same search, every match, as a CSV download")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<IncidentStatus> status,
            @RequestParam(required = false) Set<Priority> priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Boolean major,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort) {
        IncidentSearchCriteria criteria = new IncidentSearchCriteria(code, from, to, status, priority, category,
                serviceId, technicianId, slaStatus, reporterId, major, q);
        byte[] file = exporter.export(search.export(criteria, userId(jwt), role(jwt), IncidentSort.parse(sort)));
        String name = "incidents-" + LocalDate.now(clock.withZone(IncidentSearchCriteria.CAMPUS_ZONE)) + "."
                + exporter.format();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exporter.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(file);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private static Role role(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            throw new UnauthorizedOperationException("The token does not name a role.");
        }
        return Role.valueOf(roles.getFirst());
    }
}

package in.ac.bitspilani.triagegraph.topology.rca.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import in.ac.bitspilani.triagegraph.topology.rca.application.CoverageSpecificityRanker;
import in.ac.bitspilani.triagegraph.topology.rca.application.RootCauseCandidateMapper;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaAssessment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rca")
@Tag(name = "Root Cause Analysis")
@PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')")
public class RcaController {

    private final CoverageSpecificityRanker ranker;

    public RcaController(CoverageSpecificityRanker ranker) {
        this.ranker = ranker;
    }

    @PostMapping("/rank")
    @Operation(summary = "Rank likely root causes for affected services")
    public RcaRankResponse rank(@Valid @RequestBody RcaRankRequest request) {
        RcaAssessment assessment = ranker.assess(request.affectedServiceIds());
        List<RootCauseCandidate> candidates = assessment.candidates().stream()
                .map(RootCauseCandidateMapper::toContract)
                .toList();
        RootCauseCandidate topCandidate = assessment.topCandidate()
                .map(RootCauseCandidateMapper::toContract)
                .orElse(null);
        RootCauseCandidate runnerUp = assessment.runnerUp()
                .map(RootCauseCandidateMapper::toContract)
                .orElse(null);

        return new RcaRankResponse(candidates, topCandidate, runnerUp, assessment.ambiguous());
    }
}

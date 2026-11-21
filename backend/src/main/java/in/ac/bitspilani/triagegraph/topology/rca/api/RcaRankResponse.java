package in.ac.bitspilani.triagegraph.topology.rca.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcaRankResponse(
        List<RootCauseCandidate> candidates,
        RootCauseCandidate topCandidate,
        RootCauseCandidate runnerUp,
        boolean ambiguous) {

    public RcaRankResponse {
        candidates = List.copyOf(candidates);
    }
}

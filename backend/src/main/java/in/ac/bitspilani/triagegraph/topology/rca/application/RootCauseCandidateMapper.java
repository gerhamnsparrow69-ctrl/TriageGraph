package in.ac.bitspilani.triagegraph.topology.rca.application;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ScoredCandidate;

public final class RootCauseCandidateMapper {

    private RootCauseCandidateMapper() {
    }

    public static RootCauseCandidate toContract(ScoredCandidate candidate) {
        return new RootCauseCandidate(
                candidate.serviceId(),
                candidate.serviceName(),
                candidate.score(),
                candidate.coverage(),
                candidate.specificity(),
                candidate.evidence());
    }
}

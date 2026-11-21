package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.List;
import java.util.Optional;

public record RcaAssessment(List<ScoredCandidate> candidates, boolean ambiguous) {

    public RcaAssessment {
        candidates = List.copyOf(candidates);
    }

    public static RcaAssessment empty() {
        return new RcaAssessment(List.of(), false);
    }

    public Optional<ScoredCandidate> topCandidate() {
        return candidates.stream().findFirst();
    }

    public Optional<ScoredCandidate> runnerUp() {
        return candidates.stream().skip(1).findFirst();
    }
}

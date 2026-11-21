package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import java.util.List;
import java.util.Set;

/** Provided by Member 3 (topology). */
public interface RootCauseRanker {

    List<RootCauseCandidate> rank(Set<Long> affectedServiceIds);
}

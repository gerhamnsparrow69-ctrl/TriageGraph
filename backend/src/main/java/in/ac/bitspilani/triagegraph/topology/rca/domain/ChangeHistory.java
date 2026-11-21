package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ChangeHistory {

    List<ChangeInfo> recentChanges(Set<Long> serviceIds, Instant fromInclusive, Instant toInclusive);
}

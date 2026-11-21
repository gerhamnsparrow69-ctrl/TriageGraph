package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EvidenceProvider {

    Map<Long, List<Evidence>> collect(Set<Long> candidateServiceIds, EvidenceContext context);
}

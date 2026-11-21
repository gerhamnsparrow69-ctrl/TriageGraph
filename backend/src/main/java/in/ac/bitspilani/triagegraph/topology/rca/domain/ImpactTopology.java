package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.Set;

public interface ImpactTopology {

    boolean contains(long serviceId);

    Set<Long> serviceIds();

    Set<Long> dependenciesOf(long serviceId);

    Set<Long> dependentsOf(long serviceId);

    long version();
}

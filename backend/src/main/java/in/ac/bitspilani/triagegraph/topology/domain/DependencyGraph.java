package in.ac.bitspilani.triagegraph.topology.domain;

import java.util.Set;

public interface DependencyGraph {

    boolean contains(long serviceId);

    Set<Long> dependenciesOf(long serviceId);

    Set<Long> dependentsOf(long serviceId);

    Set<Long> serviceIds();

    int edgeCount();

    long version();
}

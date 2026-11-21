package in.ac.bitspilani.triagegraph.topology.domain;

import java.util.Collection;

public interface GraphBuilder {

    ServiceGraph build(Collection<Long> serviceIds, Collection<DependencyEdge> edges, long version);
}

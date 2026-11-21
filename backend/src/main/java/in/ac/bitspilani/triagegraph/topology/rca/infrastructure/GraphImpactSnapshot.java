package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import java.util.Set;

import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ImpactTopology;

public final class GraphImpactSnapshot implements ImpactTopology {

    private final ServiceGraph graph;

    GraphImpactSnapshot(ServiceGraph graph) {
        this.graph = graph;
    }

    @Override
    public boolean contains(long serviceId) {
        return graph.contains(serviceId);
    }

    @Override
    public Set<Long> serviceIds() {
        return graph.serviceIds();
    }

    @Override
    public Set<Long> dependenciesOf(long serviceId) {
        return graph.dependenciesOf(serviceId);
    }

    @Override
    public Set<Long> dependentsOf(long serviceId) {
        return graph.dependentsOf(serviceId);
    }

    @Override
    public long version() {
        return graph.version();
    }
}

package in.ac.bitspilani.triagegraph.topology.application;

import java.util.List;
import java.util.Set;

import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;

public record GraphData(Set<Long> serviceIds, List<DependencyEdge> dependencyEdges) {

    public GraphData {
        serviceIds = Set.copyOf(serviceIds);
        dependencyEdges = List.copyOf(dependencyEdges);
    }
}

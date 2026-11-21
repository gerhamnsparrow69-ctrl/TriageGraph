package in.ac.bitspilani.triagegraph.topology.infrastructure;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.exception.CyclicDependencyException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.GraphBuilder;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;

@Component
public class JGraphTGraphBuilder implements GraphBuilder {

    @Override
    public ServiceGraph build(Collection<Long> serviceIds, Collection<DependencyEdge> edges, long version) {
        Objects.requireNonNull(serviceIds, "serviceIds");
        Objects.requireNonNull(edges, "edges");

        LinkedHashSet<Long> vertices = new LinkedHashSet<>();
        for (Long serviceId : serviceIds) {
            if (serviceId == null) {
                throw new IllegalArgumentException("Service IDs cannot be null");
            }
            vertices.add(serviceId);
        }

        DirectedAcyclicGraph<Long, DefaultEdge> graph =
                new DirectedAcyclicGraph<>(DefaultEdge.class);
        vertices.forEach(graph::addVertex);

        LinkedHashSet<DependencyEdge> distinctEdges = new LinkedHashSet<>();
        for (DependencyEdge edge : edges) {
            Objects.requireNonNull(edge, "dependency edge");
            if (!vertices.contains(edge.dependentId())) {
                throw new ServiceNotFoundException(
                        "Unknown service id " + edge.dependentId() + " in dependency edge");
            }
            if (!vertices.contains(edge.dependencyId())) {
                throw new ServiceNotFoundException(
                        "Unknown service id " + edge.dependencyId() + " in dependency edge");
            }
            if (edge.dependentId() == edge.dependencyId()) {
                throw new CyclicDependencyException(
                        "A service cannot depend on itself: " + edge.dependentId());
            }
            if (!distinctEdges.add(edge)) {
                continue;
            }
            try {
                graph.addEdge(edge.dependentId(), edge.dependencyId());
            } catch (IllegalArgumentException ex) {
                throw new CyclicDependencyException(
                        "Dependency edge " + edge.dependentId() + " -> " + edge.dependencyId()
                                + " creates a cycle",
                        ex);
            }
        }

        return new ServiceGraph(vertices, distinctEdges, version);
    }
}

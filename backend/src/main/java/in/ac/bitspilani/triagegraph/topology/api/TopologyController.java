package in.ac.bitspilani.triagegraph.topology.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.topology.application.GraphRegistry;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/topology")
public class TopologyController {

    private static final String TOPOLOGY_READ_ROLES =
            "hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')";

    private final GraphRegistry graphs;
    private final BlastRadiusProvider blastRadius;

    public TopologyController(GraphRegistry graphs, BlastRadiusProvider blastRadius) {
        this.graphs = graphs;
        this.blastRadius = blastRadius;
    }

    @GetMapping
    @PreAuthorize(TOPOLOGY_READ_ROLES)
    public GraphResponse graph() {
        ServiceGraph graph = graphs.currentGraph();
        return new GraphResponse(
                List.copyOf(graph.serviceIds()),
                List.copyOf(graph.edges()),
                graph.edgeCount(),
                graph.version());
    }

    @GetMapping("/services/{serviceId}/dependencies")
    @PreAuthorize(TOPOLOGY_READ_ROLES)
    public RelationshipsResponse dependencies(@PathVariable @Positive long serviceId) {
        ServiceGraph graph = graphs.currentGraph();
        return new RelationshipsResponse(
                serviceId,
                List.copyOf(graph.directDependenciesOf(serviceId)),
                List.copyOf(graph.dependenciesOf(serviceId)));
    }

    @GetMapping("/services/{serviceId}/dependents")
    @PreAuthorize(TOPOLOGY_READ_ROLES)
    public RelationshipsResponse dependents(@PathVariable @Positive long serviceId) {
        ServiceGraph graph = graphs.currentGraph();
        return new RelationshipsResponse(
                serviceId,
                List.copyOf(graph.directDependentsOf(serviceId)),
                List.copyOf(graph.dependentsOf(serviceId)));
    }

    @GetMapping("/services/{serviceId}/blast-radius")
    @PreAuthorize(TOPOLOGY_READ_ROLES)
    public BlastRadiusResponse blastRadius(@PathVariable @Positive long serviceId) {
        return new BlastRadiusResponse(serviceId, blastRadius.dependentCount(serviceId));
    }

    public record GraphResponse(List<Long> serviceIds, List<DependencyEdge> edges,
            int edgeCount, long version) {
    }

    public record RelationshipsResponse(long serviceId, List<Long> directServiceIds,
            List<Long> transitiveServiceIds) {
    }

    public record BlastRadiusResponse(long serviceId, int dependentCount) {
    }
}

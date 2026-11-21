package in.ac.bitspilani.triagegraph.topology.infrastructure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.domain.ServiceDependency;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ServiceDependencyRepository;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.application.GraphData;
import in.ac.bitspilani.triagegraph.topology.application.GraphSource;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;

@Component
public class CatalogGraphSource implements GraphSource {

    private final ITServiceRepository services;
    private final ServiceDependencyRepository dependencies;

    public CatalogGraphSource(ITServiceRepository services, ServiceDependencyRepository dependencies) {
        this.services = services;
        this.dependencies = dependencies;
    }

    @Override
    @Transactional(readOnly = true)
    public GraphData load() {
        Set<Long> serviceIds = new LinkedHashSet<>();
        services.findAll().forEach(service -> {
            if (service.getId() == null) {
                throw new ServiceNotFoundException("Catalogue contains an unsaved service");
            }
            serviceIds.add(service.getId());
        });

        List<DependencyEdge> edges = dependencies.findAll().stream()
                .map(this::toEdge)
                .toList();
        for (DependencyEdge edge : edges) {
            if (!serviceIds.contains(edge.dependentId())) {
                throw new ServiceNotFoundException(
                        "Dependency references unknown dependent service id " + edge.dependentId());
            }
            if (!serviceIds.contains(edge.dependencyId())) {
                throw new ServiceNotFoundException(
                        "Dependency references unknown dependency service id " + edge.dependencyId());
            }
        }
        return new GraphData(serviceIds, edges);
    }

    private DependencyEdge toEdge(ServiceDependency dependency) {
        Long dependentId = dependency.getDependent().getId();
        Long dependencyId = dependency.getDependency().getId();
        if (dependentId == null || dependencyId == null) {
            throw new ServiceNotFoundException("Dependency references an unsaved service");
        }
        return new DependencyEdge(dependentId, dependencyId);
    }
}

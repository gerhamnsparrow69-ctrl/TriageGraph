package in.ac.bitspilani.triagegraph.topology.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.exception.CyclicDependencyException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

public final class ServiceGraph implements DependencyGraph {

    private final Set<Long> serviceIds;
    private final Set<DependencyEdge> edges;
    private final Map<Long, Set<Long>> dependencies;
    private final Map<Long, Set<Long>> dependents;
    private final long version;

    public ServiceGraph(Collection<Long> serviceIds, Collection<DependencyEdge> edges, long version) {
        LinkedHashSet<Long> vertexCopy = new LinkedHashSet<>(serviceIds);
        LinkedHashSet<DependencyEdge> edgeCopy = new LinkedHashSet<>(edges);
        Map<Long, Set<Long>> dependencyMap = new LinkedHashMap<>();
        Map<Long, Set<Long>> dependentMap = new LinkedHashMap<>();

        vertexCopy.forEach(id -> {
            dependencyMap.put(id, new LinkedHashSet<>());
            dependentMap.put(id, new LinkedHashSet<>());
        });
        for (DependencyEdge edge : edgeCopy) {
            if (!vertexCopy.contains(edge.dependentId())) {
                throw new ServiceNotFoundException("Unknown service id " + edge.dependentId()
                        + " in dependency edge");
            }
            if (!vertexCopy.contains(edge.dependencyId())) {
                throw new ServiceNotFoundException("Unknown service id " + edge.dependencyId()
                        + " in dependency edge");
            }
            if (edge.dependentId() == edge.dependencyId()) {
                throw new CyclicDependencyException("Service " + edge.dependentId()
                        + " cannot depend on itself");
            }
            dependencyMap.get(edge.dependentId()).add(edge.dependencyId());
            dependentMap.get(edge.dependencyId()).add(edge.dependentId());
        }
        validateAcyclic(vertexCopy, dependencyMap);

        this.serviceIds = Collections.unmodifiableSet(vertexCopy);
        this.edges = Collections.unmodifiableSet(edgeCopy);
        this.dependencies = immutableAdjacency(dependencyMap);
        this.dependents = immutableAdjacency(dependentMap);
        this.version = version;
    }

    @Override
    public boolean contains(long serviceId) {
        return serviceIds.contains(serviceId);
    }

    @Override
    public Set<Long> dependenciesOf(long serviceId) {
        requireKnown(serviceId);
        return transitive(serviceId, dependencies);
    }

    @Override
    public Set<Long> dependentsOf(long serviceId) {
        requireKnown(serviceId);
        return transitive(serviceId, dependents);
    }

    @Override
    public Set<Long> serviceIds() {
        return serviceIds;
    }

    public Set<DependencyEdge> edges() {
        return edges;
    }

    @Override
    public int edgeCount() {
        return edges.size();
    }

    @Override
    public long version() {
        return version;
    }

    private void requireKnown(long serviceId) {
        if (!contains(serviceId)) {
            throw new ServiceNotFoundException("No service with id " + serviceId + " exists in the graph");
        }
    }

    private Set<Long> transitive(long start, Map<Long, Set<Long>> adjacency) {
        LinkedHashSet<Long> visited = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>(adjacency.get(start));
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (current != start && visited.add(current)) {
                queue.addAll(adjacency.get(current));
            }
        }
        visited.remove(start);
        return Collections.unmodifiableSet(visited);
    }

    private static Map<Long, Set<Long>> immutableAdjacency(Map<Long, Set<Long>> source) {
        Map<Long, Set<Long>> copy = new LinkedHashMap<>();
        source.forEach((id, adjacent) ->
                copy.put(id, Collections.unmodifiableSet(new LinkedHashSet<>(adjacent))));
        return Collections.unmodifiableMap(copy);
    }

    private static void validateAcyclic(Set<Long> vertices, Map<Long, Set<Long>> dependencies) {
        Map<Long, Integer> incomingCounts = new LinkedHashMap<>();
        vertices.forEach(id -> incomingCounts.put(id, 0));
        dependencies.values().forEach(adjacent ->
                adjacent.forEach(id -> incomingCounts.compute(id, (ignored, count) -> count + 1)));

        Queue<Long> ready = new ArrayDeque<>();
        incomingCounts.forEach((id, count) -> {
            if (count == 0) {
                ready.add(id);
            }
        });

        int visitedCount = 0;
        while (!ready.isEmpty()) {
            Long current = ready.remove();
            visitedCount++;
            for (Long dependency : dependencies.get(current)) {
                int remainingIncoming = incomingCounts.compute(dependency,
                        (ignored, count) -> count - 1);
                if (remainingIncoming == 0) {
                    ready.add(dependency);
                }
            }
        }

        if (visitedCount != vertices.size()) {
            List<Long> cyclicServices = new ArrayList<>();
            incomingCounts.forEach((id, count) -> {
                if (count > 0) {
                    cyclicServices.add(id);
                }
            });
            throw new CyclicDependencyException("Dependency cycle detected involving services "
                    + cyclicServices);
        }
    }
}

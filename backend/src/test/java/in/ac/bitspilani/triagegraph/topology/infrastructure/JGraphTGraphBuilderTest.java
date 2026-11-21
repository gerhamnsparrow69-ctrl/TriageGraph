package in.ac.bitspilani.triagegraph.topology.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.CyclicDependencyException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;

class JGraphTGraphBuilderTest {

    private final JGraphTGraphBuilder builder = new JGraphTGraphBuilder();

    @Test
    void buildsCompleteCatalogTopologyWithExpectedDirectEdgesAndCounts() {
        ServiceGraph graph = completeGraph();

        assertThat(graph.serviceIds()).hasSize(17);
        assertThat(graph.edgeCount()).isEqualTo(29);
        assertThat(graph.edges()).containsExactlyInAnyOrderElementsOf(edges());
        assertThat(graph.dependenciesOf(1)).hasSize(7).contains(2L, 3L, 4L, 5L, 6L, 7L, 8L);
    }

    @Test
    void computesTransitiveDependenciesAndDependents() {
        ServiceGraph graph = builder.build(
                Set.of(1L, 2L, 3L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(2, 3)),
                1);

        assertThat(graph.dependenciesOf(1)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(graph.dependentsOf(3)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void computesDiamondRelationshipsAndCountsMultiplePathsOnce() {
        ServiceGraph graph = builder.build(
                Set.of(1L, 2L, 3L, 4L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(1, 3),
                        new DependencyEdge(2, 4), new DependencyEdge(3, 4)),
                1);

        assertThat(graph.dependenciesOf(1)).containsExactlyInAnyOrder(2L, 3L, 4L);
        assertThat(graph.dependentsOf(4)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void isolatedServiceHasNoRelationships() {
        ServiceGraph graph = builder.build(Set.of(1L, 2L), List.of(), 1);

        assertThat(graph.dependenciesOf(2)).isEmpty();
        assertThat(graph.dependentsOf(2)).isEmpty();
    }

    @Test
    void duplicateEdgesDoNotInflateCounts() {
        ServiceGraph graph = builder.build(
                Set.of(1L, 2L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(1, 2)),
                1);

        assertThat(graph.edgeCount()).isEqualTo(1);
        assertThat(graph.dependentsOf(2)).containsExactly(1L);
    }

    @Test
    void rejectsSelfLoopsAndCycles() {
        assertThatThrownBy(() -> builder.build(
                Set.of(1L), List.of(new DependencyEdge(1, 1)), 1))
                .isInstanceOf(CyclicDependencyException.class);
        assertThatThrownBy(() -> builder.build(
                Set.of(1L, 2L, 3L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(2, 3),
                        new DependencyEdge(3, 1)),
                1))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void directConstructionRejectsSelfLoopsAndCycles() {
        assertThatThrownBy(() -> new ServiceGraph(
                Set.of(1L), List.of(new DependencyEdge(1, 1)), 1))
                .isInstanceOf(CyclicDependencyException.class);
        assertThatThrownBy(() -> new ServiceGraph(
                Set.of(1L, 2L, 3L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(2, 3),
                        new DependencyEdge(3, 1)),
                1))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void rejectsDanglingEdgesAndUnknownServiceQueries() {
        assertThatThrownBy(() -> builder.build(
                Set.of(1L), List.of(new DependencyEdge(1, 2)), 1))
                .isInstanceOf(ServiceNotFoundException.class);

        ServiceGraph graph = builder.build(Set.of(1L), List.of(), 1);
        assertThatThrownBy(() -> graph.dependenciesOf(99))
                .isInstanceOf(ServiceNotFoundException.class);
        assertThatThrownBy(() -> graph.dependentsOf(99))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void snapshotCollectionsAreImmutable() {
        ServiceGraph graph = completeGraph();
        assertThatThrownBy(() -> graph.serviceIds().add(100L))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.edges().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.dependenciesOf(1).clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.dependentsOf(5).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ServiceGraph completeGraph() {
        return builder.build(new LinkedHashSet<>(range(1, 17)), edges(), 1);
    }

    private List<Long> range(int first, int last) {
        List<Long> ids = new ArrayList<>();
        for (long id = first; id <= last; id++) {
            ids.add(id);
        }
        return ids;
    }

    private List<DependencyEdge> edges() {
        return List.of(
                new DependencyEdge(1, 2), new DependencyEdge(1, 3), new DependencyEdge(1, 8),
                new DependencyEdge(2, 5), new DependencyEdge(2, 7),
                new DependencyEdge(9, 3), new DependencyEdge(9, 8), new DependencyEdge(9, 15),
                new DependencyEdge(10, 3), new DependencyEdge(10, 8),
                new DependencyEdge(11, 3), new DependencyEdge(11, 7),
                new DependencyEdge(14, 3),
                new DependencyEdge(12, 5), new DependencyEdge(12, 3),
                new DependencyEdge(13, 5), new DependencyEdge(13, 3),
                new DependencyEdge(16, 3), new DependencyEdge(16, 5),
                new DependencyEdge(17, 5),
                new DependencyEdge(3, 4), new DependencyEdge(3, 5),
                new DependencyEdge(4, 6), new DependencyEdge(4, 5),
                new DependencyEdge(8, 6), new DependencyEdge(8, 5),
                new DependencyEdge(15, 5), new DependencyEdge(7, 5),
                new DependencyEdge(5, 6));
    }
}

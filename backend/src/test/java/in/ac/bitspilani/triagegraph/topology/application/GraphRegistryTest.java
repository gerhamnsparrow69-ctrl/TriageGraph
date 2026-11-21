package in.ac.bitspilani.triagegraph.topology.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.events.CatalogChanged;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;
import in.ac.bitspilani.triagegraph.topology.infrastructure.JGraphTGraphBuilder;

class GraphRegistryTest {

    @Test
    void lazilyInitializesAndIncrementsVersionOnlyOnSuccessfulReload() {
        AtomicInteger loads = new AtomicInteger();
        GraphRegistry registry = new GraphRegistry(() -> {
            loads.incrementAndGet();
            return graphData(1, 2);
        }, new JGraphTGraphBuilder());

        assertThat(loads).hasValue(0);
        ServiceGraph first = registry.currentGraph();
        assertThat(first.version()).isEqualTo(1);
        assertThat(registry.currentGraph()).isSameAs(first);
        assertThat(loads).hasValue(1);

        ServiceGraph second = registry.reload();
        assertThat(second.version()).isEqualTo(2);
        assertThat(loads).hasValue(2);
    }

    @Test
    void failedReloadPreservesPreviousSnapshotAndVersion() {
        AtomicInteger loads = new AtomicInteger();
        GraphRegistry registry = new GraphRegistry(() -> {
            if (loads.incrementAndGet() == 1) {
                return graphData(1, 2);
            }
            throw new IllegalStateException("catalog unavailable");
        }, new JGraphTGraphBuilder());

        ServiceGraph previous = registry.currentGraph();
        assertThatThrownBy(registry::reload).isInstanceOf(IllegalStateException.class);
        assertThat(registry.currentGraph()).isSameAs(previous);
        assertThat(registry.currentGraph().version()).isEqualTo(1);
    }

    @Test
    void catalogChangedReloadsGraph() {
        AtomicInteger loads = new AtomicInteger();
        GraphRegistry registry = new GraphRegistry(() -> {
            loads.incrementAndGet();
            return graphData(1, 2);
        }, new JGraphTGraphBuilder());
        GraphReloadListener listener = new GraphReloadListener(registry);

        ServiceGraph before = registry.currentGraph();
        listener.onCatalogChanged(new CatalogChanged(null, Instant.EPOCH, "DEPENDENCY", "1", "added"));

        assertThat(registry.currentGraph().version()).isEqualTo(before.version() + 1);
        assertThat(loads).hasValue(2);
    }

    @Test
    void concurrentReadersOnlySeeCompleteSnapshots() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        GraphRegistry registry = new GraphRegistry(() -> {
            int load = loads.incrementAndGet();
            return load % 2 == 1 ? graphData(1, 2) : graphData(3, 4);
        }, new JGraphTGraphBuilder());
        registry.currentGraph();

        int readerCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount);
        CountDownLatch ready = new CountDownLatch(readerCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<? extends Future<?>> readers = java.util.stream.IntStream.range(0, readerCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        for (int i = 0; i < 500; i++) {
                            ServiceGraph snapshot = registry.currentGraph();
                            if (snapshot.serviceIds().equals(Set.of(1L, 2L))) {
                                assertThat(snapshot.edges()).containsExactly(new DependencyEdge(1, 2));
                                assertThat(snapshot.dependenciesOf(1)).containsExactly(2L);
                            } else {
                                assertThat(snapshot.serviceIds()).isEqualTo(Set.of(3L, 4L));
                                assertThat(snapshot.edges()).containsExactly(new DependencyEdge(3, 4));
                                assertThat(snapshot.dependenciesOf(3)).containsExactly(4L);
                            }
                        }
                        return null;
                    })).toList();

            ready.await();
            start.countDown();
            for (int i = 0; i < 100; i++) {
                registry.reload();
            }
            for (Future<?> reader : readers) {
                reader.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private GraphData graphData(long first, long second) {
        return new GraphData(Set.of(first, second),
                List.of(new DependencyEdge(first, second)));
    }
}

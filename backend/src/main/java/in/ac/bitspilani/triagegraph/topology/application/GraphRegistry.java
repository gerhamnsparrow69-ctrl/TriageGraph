package in.ac.bitspilani.triagegraph.topology.application;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.topology.domain.GraphBuilder;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;

@Component
public class GraphRegistry {

    private final GraphSource source;
    private final GraphBuilder builder;
    private final AtomicReference<ServiceGraph> current = new AtomicReference<>();

    public GraphRegistry(GraphSource source, GraphBuilder builder) {
        this.source = source;
        this.builder = builder;
    }

    public ServiceGraph currentGraph() {
        ServiceGraph snapshot = current.get();
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            snapshot = current.get();
            return snapshot != null ? snapshot : reload();
        }
    }

    public synchronized ServiceGraph reload() {
        ServiceGraph previous = current.get();
        long nextVersion = previous == null ? 1 : previous.version() + 1;
        GraphData data = source.load();
        ServiceGraph replacement = builder.build(data.serviceIds(), data.dependencyEdges(), nextVersion);
        current.set(replacement);
        return replacement;
    }
}

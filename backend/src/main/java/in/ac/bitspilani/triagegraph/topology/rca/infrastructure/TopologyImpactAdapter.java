package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.topology.application.GraphRegistry;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ImpactTopology;

@Component
public class TopologyImpactAdapter {

    private final GraphRegistry graphs;

    public TopologyImpactAdapter(GraphRegistry graphs) {
        this.graphs = graphs;
    }

    public ImpactTopology snapshot() {
        return new GraphImpactSnapshot(graphs.currentGraph());
    }
}

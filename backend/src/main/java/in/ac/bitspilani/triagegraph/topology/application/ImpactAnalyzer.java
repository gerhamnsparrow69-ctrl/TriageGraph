package in.ac.bitspilani.triagegraph.topology.application;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;

@Service
public class ImpactAnalyzer implements BlastRadiusProvider {

    private final GraphRegistry graphs;

    public ImpactAnalyzer(GraphRegistry graphs) {
        this.graphs = graphs;
    }

    @Override
    public int dependentCount(long serviceId) {
        return graphs.currentGraph().dependentsOf(serviceId).size();
    }
}

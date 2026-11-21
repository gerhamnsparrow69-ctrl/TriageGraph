package in.ac.bitspilani.triagegraph.topology.rca.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.RootCauseRanker;
import in.ac.bitspilani.triagegraph.topology.rca.domain.AggregatedEvidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.CandidateGenerator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ImpactTopology;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaAssessment;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaScorer;
import in.ac.bitspilani.triagegraph.topology.rca.infrastructure.TopologyImpactAdapter;

@Service
public class CoverageSpecificityRanker implements RootCauseRanker {

    private static final Logger log = LoggerFactory.getLogger(CoverageSpecificityRanker.class);

    private final CandidateGenerator candidateGenerator;
    private final TopologyImpactAdapter topology;
    private final CatalogQuery catalog;
    private final EvidenceCollector evidenceCollector;
    private final RcaScorer scorer;

    public CoverageSpecificityRanker(
            CandidateGenerator candidateGenerator,
            TopologyImpactAdapter topology,
            CatalogQuery catalog,
            EvidenceCollector evidenceCollector,
            RcaScorer scorer) {
        this.candidateGenerator = candidateGenerator;
        this.topology = topology;
        this.catalog = catalog;
        this.evidenceCollector = evidenceCollector;
        this.scorer = scorer;
    }

    @Override
    public List<RootCauseCandidate> rank(Set<Long> affectedServiceIds) {
        return assess(affectedServiceIds).candidates().stream()
                .map(RootCauseCandidateMapper::toContract)
                .toList();
    }

    public RcaAssessment assess(Set<Long> affectedServiceIds) {
        Set<Long> affected = candidateGenerator.validateInput(affectedServiceIds);
        if (affected.isEmpty()) {
            return RcaAssessment.empty();
        }

        ImpactTopology snapshot = topology.snapshot();
        List<Long> candidateIds = candidateGenerator.generate(affected, snapshot);
        Map<Long, String> serviceNames = new HashMap<>();
        for (ServiceRef service : catalog.services()) {
            if (service.name() != null && !service.name().isBlank()) {
                serviceNames.putIfAbsent(service.id(), service.name());
            }
        }
        for (long serviceId : candidateIds) {
            if (!serviceNames.containsKey(serviceId)) {
                log.warn("No catalogue display name found for service {}; using fallback name", serviceId);
            }
        }

        Map<Long, AggregatedEvidence> evidence = evidenceCollector.collect(candidateIds);
        return scorer.assess(candidateIds, affected, snapshot, serviceNames, evidence);
    }

}

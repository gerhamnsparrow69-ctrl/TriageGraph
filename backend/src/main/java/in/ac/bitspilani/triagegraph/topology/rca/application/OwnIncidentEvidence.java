package in.ac.bitspilani.triagegraph.topology.rca.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.topology.rca.domain.Evidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceContext;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceProvider;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceType;
import in.ac.bitspilani.triagegraph.topology.rca.domain.OpenIncidentLookup;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaConfig;

@Component
public class OwnIncidentEvidence implements EvidenceProvider {

    private final OpenIncidentLookup incidents;
    private final RcaConfig config;

    public OwnIncidentEvidence(OpenIncidentLookup incidents, RcaConfig config) {
        this.incidents = incidents;
        this.config = config;
    }

    @Override
    public Map<Long, List<Evidence>> collect(Set<Long> candidateServiceIds, EvidenceContext context) {
        if (candidateServiceIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> incidentServiceIds = incidents.openIncidentServiceIds();
        Map<Long, List<Evidence>> result = new HashMap<>();
        for (long serviceId : candidateServiceIds) {
            if (incidentServiceIds.contains(serviceId)) {
                result.put(serviceId, List.of(new Evidence(
                        EvidenceType.OPEN_INCIDENT,
                        config.openIncidentBonus(),
                        "Open incident on this service")));
            }
        }
        return Map.copyOf(result);
    }
}

package in.ac.bitspilani.triagegraph.topology.rca.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.topology.rca.domain.AggregatedEvidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.Evidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceAggregator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceContext;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceProvider;

@Component
public class EvidenceCollector {

    private final List<EvidenceProvider> providers;
    private final EvidenceAggregator aggregator;
    private final Clock clock;

    public EvidenceCollector(
            List<EvidenceProvider> providers,
            EvidenceAggregator aggregator,
            Clock clock) {
        this.providers = List.copyOf(providers);
        this.aggregator = aggregator;
        this.clock = clock;
    }

    public Map<Long, AggregatedEvidence> collect(Collection<Long> candidateServiceIds) {
        TreeSet<Long> orderedIds = new TreeSet<>(candidateServiceIds);
        if (orderedIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> ids = Collections.unmodifiableSet(orderedIds);
        EvidenceContext context = new EvidenceContext(clock.instant());
        Map<Long, List<Evidence>> byService = new LinkedHashMap<>();
        for (long serviceId : ids) {
            byService.put(serviceId, new ArrayList<>());
        }

        for (EvidenceProvider provider : providers) {
            Map<Long, List<Evidence>> supplied = provider.collect(ids, context);
            supplied.forEach((serviceId, evidence) -> {
                List<Evidence> target = byService.get(serviceId);
                if (target != null) {
                    target.addAll(evidence);
                }
            });
        }

        Map<Long, AggregatedEvidence> aggregated = new LinkedHashMap<>();
        byService.forEach((serviceId, evidence) ->
                aggregated.put(serviceId, aggregator.aggregate(evidence)));
        return Collections.unmodifiableMap(aggregated);
    }
}

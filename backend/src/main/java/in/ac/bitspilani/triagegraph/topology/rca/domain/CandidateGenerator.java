package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

public final class CandidateGenerator {

    public Set<Long> validateInput(Set<Long> affectedServiceIds) {
        if (affectedServiceIds == null) {
            throw new InvalidInputException("Affected service IDs are required.");
        }
        for (Long serviceId : affectedServiceIds) {
            if (serviceId == null) {
                throw new InvalidInputException("Affected service IDs cannot contain null.");
            }
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(new TreeSet<>(affectedServiceIds)));
    }

    public List<Long> generate(Set<Long> affectedServiceIds, ImpactTopology topology) {
        Set<Long> affected = validateInput(affectedServiceIds);
        for (long serviceId : affected) {
            if (!topology.contains(serviceId)) {
                throw new ServiceNotFoundException("No service with id " + serviceId + " exists in the graph");
            }
        }

        TreeSet<Long> candidates = new TreeSet<>(affected);
        for (long serviceId : affected) {
            candidates.addAll(topology.dependenciesOf(serviceId));
        }
        return List.copyOf(candidates);
    }
}

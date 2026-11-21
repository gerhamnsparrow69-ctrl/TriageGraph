package in.ac.bitspilani.triagegraph.topology.rca.application;

import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeHistory;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeInfo;
import in.ac.bitspilani.triagegraph.topology.rca.domain.Evidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceContext;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceProvider;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceType;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaConfig;

@Component
public class RecentChangeEvidence implements EvidenceProvider {

    private final ChangeHistory changes;
    private final RcaConfig config;

    public RecentChangeEvidence(ChangeHistory changes, RcaConfig config) {
        this.changes = changes;
        this.config = config;
    }

    @Override
    public Map<Long, List<Evidence>> collect(Set<Long> candidateServiceIds, EvidenceContext context) {
        if (candidateServiceIds.isEmpty()) {
            return Map.of();
        }

        Instant fromInclusive = context.now().minus(config.recentChangeWindow());
        Map<Long, ChangeInfo> mostRecentByService = new HashMap<>();
        Comparator<ChangeInfo> preferred = Comparator
                .comparing(ChangeInfo::loggedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(change -> change.description() == null ? "" : change.description());

        for (ChangeInfo change : changes.recentChanges(candidateServiceIds, fromInclusive, context.now())) {
            if (change == null || change.loggedAt() == null
                    || change.loggedAt().isBefore(fromInclusive)
                    || change.loggedAt().isAfter(context.now())
                    || !candidateServiceIds.contains(change.serviceId())) {
                continue;
            }
            ChangeInfo current = mostRecentByService.get(change.serviceId());
            if (current == null || preferred.compare(change, current) < 0) {
                mostRecentByService.put(change.serviceId(), change);
            }
        }

        Map<Long, List<Evidence>> result = new HashMap<>();
        mostRecentByService.forEach((serviceId, change) -> {
            long minutesAgo = Duration.between(change.loggedAt(), context.now()).toMinutes();
            String text = String.format(Locale.ROOT, "Change logged %d min ago: %s",
                    minutesAgo, change.description());
            result.put(serviceId, List.of(new Evidence(
                    EvidenceType.RECENT_CHANGE, config.recentChangeBonus(), text)));
        });
        return Map.copyOf(result);
    }
}

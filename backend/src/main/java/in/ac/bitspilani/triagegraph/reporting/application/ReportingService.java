package in.ac.bitspilani.triagegraph.reporting.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.reporting.application.Reports.AiEffectiveness;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.Dashboard;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.Overview;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.ServiceAnalysis;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.SlaByPriority;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.TechnicianWorkload;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.Trend;
import in.ac.bitspilani.triagegraph.reporting.application.Reports.TrendDay;
import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact;
import in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

/** Builds the six reports (blueprint §10.4) from the read model. Every number is worked out at clock.instant(). */
@Service
public class ReportingService {

    public static final int TREND_DAYS = 14;
    private static final int TOP_ROOT_CAUSES = 3;

    private final IncidentReadModel readModel;
    private final Clock clock;

    public ReportingService(IncidentReadModel readModel, Clock clock) {
        this.readModel = readModel;
        this.clock = clock;
    }

    public Dashboard dashboard() {
        Instant now = clock.instant();
        List<IncidentFact> incidents = readModel.incidents();
        return new Dashboard(now, overview(incidents, now), technicianWorkload(incidents, now),
                serviceAnalysis(incidents), trend(incidents, now), slaByPriority(incidents, now),
                aiEffectiveness(incidents));
    }

    public Overview overview() {
        return overview(readModel.incidents(), clock.instant());
    }

    Overview overview(List<IncidentFact> incidents, Instant now) {
        long open = count(incidents, IncidentFact::isOpen);
        long criticalOpen = count(incidents, i -> i.isOpen() && i.isCritical());
        long resolved = count(incidents, i -> i.status() == IncidentStatus.RESOLVED || i.status() == IncidentStatus.VERIFIED);
        long closed = count(incidents, i -> i.status() == IncidentStatus.CLOSED);
        long breached = count(incidents, i -> i.slaBreached(now));
        long met = count(incidents, i -> slaJudged(i) && i.slaStatus(now) == SlaStatus.MET);
        Double compliance = met + breached == 0 ? null : round(100.0 * met / (met + breached));
        return new Overview(incidents.size(), open, criticalOpen, resolved, closed, breached, compliance,
                averageMinutes(incidents.stream().map(IncidentFact::timeToAcknowledge)),
                averageMinutes(incidents.stream().filter(IncidentFact::isResolved).map(IncidentFact::timeToResolve)));
    }

    List<TechnicianWorkload> technicianWorkload(List<IncidentFact> incidents, Instant now) {
        Instant weekAgo = now.minus(Duration.ofDays(7));
        Map<Long, List<IncidentFact>> byAssignee = incidents.stream()
                .filter(i -> i.assigneeId() != null)
                .collect(Collectors.groupingBy(IncidentFact::assigneeId));
        return readModel.technicians().stream()
                .map(t -> workload(t, byAssignee.getOrDefault(t.id(), List.of()), weekAgo))
                .sorted(Comparator.comparingLong(TechnicianWorkload::active).reversed()
                        .thenComparing(TechnicianWorkload::name))
                .toList();
    }

    private static TechnicianWorkload workload(TechnicianFact t, List<IncidentFact> theirs, Instant weekAgo) {
        long active = count(theirs, IncidentFact::isOpen);
        long resolvedThisWeek = count(theirs, i -> i.isResolved() && !i.resolvedAt().isBefore(weekAgo));
        Double avg = averageMinutes(theirs.stream().filter(IncidentFact::isResolved).map(IncidentFact::timeToResolve));
        return new TechnicianWorkload(t.id(), t.name(), t.role(), active, t.maxLoad(), t.available(),
                resolvedThisWeek, avg);
    }

    List<ServiceAnalysis> serviceAnalysis(List<IncidentFact> incidents) {
        Map<Long, List<IncidentFact>> byService = incidents.stream()
                .filter(i -> i.serviceId() != null)
                .collect(Collectors.groupingBy(IncidentFact::serviceId));
        return readModel.services().stream()
                .map(s -> analyse(s, byService.getOrDefault(s.id(), List.of())))
                .sorted(Comparator.comparingLong(ServiceAnalysis::incidents).reversed()
                        .thenComparing(ServiceAnalysis::name))
                .toList();
    }

    private static ServiceAnalysis analyse(ServiceFact s, List<IncidentFact> theirs) {
        long open = count(theirs, IncidentFact::isOpen);
        boolean outage = theirs.stream().anyMatch(i -> i.isOpen() && i.isCritical());
        String health = outage ? "OUTAGE" : open > 0 ? "DEGRADED" : "HEALTHY";
        List<String> causes = theirs.stream()
                .map(IncidentFact::rootCause)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_ROOT_CAUSES)
                .map(Map.Entry::getKey)
                .toList();
        return new ServiceAnalysis(s.id(), s.name(), s.criticality(), theirs.size(), open, health, causes);
    }

    Trend trend(List<IncidentFact> incidents, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, IncidentSearchCriteria.CAMPUS_ZONE);
        LocalDate first = today.minusDays(TREND_DAYS - 1L);
        Function<IncidentFact, LocalDate> day = i -> LocalDate.ofInstant(i.createdAt(), IncidentSearchCriteria.CAMPUS_ZONE);
        List<IncidentFact> window = incidents.stream().filter(i -> !day.apply(i).isBefore(first)).toList();
        List<String> categories = new ArrayList<>(new TreeSet<>(window.stream().map(ReportingService::categoryOf).toList()));
        List<TrendDay> days = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            LocalDate date = d;
            Map<String, Long> counts = new LinkedHashMap<>();
            categories.forEach(c -> counts.put(c, 0L));
            window.stream().filter(i -> day.apply(i).equals(date))
                    .forEach(i -> counts.merge(categoryOf(i), 1L, Long::sum));
            days.add(new TrendDay(date, counts, counts.values().stream().mapToLong(Long::longValue).sum()));
        }
        return new Trend(categories, days);
    }

    List<SlaByPriority> slaByPriority(List<IncidentFact> incidents, Instant now) {
        List<SlaByPriority> result = new ArrayList<>();
        for (Priority p : Priority.values()) {
            List<IncidentFact> ofP = incidents.stream().filter(i -> i.priority() == p && slaJudged(i)).toList();
            long met = count(ofP, i -> i.slaStatus(now) == SlaStatus.MET);
            long breached = count(ofP, i -> i.slaBreached(now));
            long onTrack = count(ofP, i -> i.isOpen() && !i.slaBreached(now));
            result.add(new SlaByPriority(p.name(), met, breached, onTrack));
        }
        return result;
    }

    AiEffectiveness aiEffectiveness(List<IncidentFact> incidents) {
        long classified = count(incidents, i -> i.classificationSource() != null);
        long accepted = count(incidents, IncidentFact::wasSuggestionAccepted);
        long joined = incidents.stream().mapToLong(i -> i.followerIds().size()).sum();
        long merged = count(incidents, i -> i.status() == IncidentStatus.MERGED);
        long majors = incidents.stream().map(IncidentFact::majorIncidentId).filter(Objects::nonNull).distinct().count();
        Double acceptance = classified == 0 ? null : round(100.0 * accepted / classified);
        return new AiEffectiveness(acceptance, classified, joined, merged, joined + merged, majors);
    }

    /** A merged duplicate never ran its own SLA, so it is left out of compliance figures. */
    private static boolean slaJudged(IncidentFact i) {
        return i.resolutionDueAt() != null && i.status() != IncidentStatus.MERGED;
    }

    private static String categoryOf(IncidentFact i) {
        return i.categoryCode() == null || i.categoryCode().isBlank() ? "UNCLASSIFIED" : i.categoryCode();
    }

    private static long count(List<IncidentFact> incidents, java.util.function.Predicate<IncidentFact> test) {
        return incidents.stream().filter(test).count();
    }

    private static Double averageMinutes(Stream<Optional<Duration>> durations) {
        OptionalDouble avg = durations.flatMap(Optional::stream).mapToLong(Duration::toSeconds).average();
        return avg.isPresent() ? round(avg.getAsDouble() / 60.0) : null;
    }

    private static Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

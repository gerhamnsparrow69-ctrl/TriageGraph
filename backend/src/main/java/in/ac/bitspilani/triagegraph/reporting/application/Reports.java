package in.ac.bitspilani.triagegraph.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The six reports of blueprint §10.4, as the JSON the reports page draws. */
public final class Reports {

    private Reports() {
    }

    /** All six at once, so the page needs one request. */
    public record Dashboard(Instant generatedAt, Overview overview, List<TechnicianWorkload> technicians,
            List<ServiceAnalysis> services, Trend trend, List<SlaByPriority> slaByPriority, AiEffectiveness ai) {
    }

    /** KPI tiles. Times are in minutes; null when nothing has been acknowledged or resolved yet. */
    public record Overview(long total, long open, long criticalOpen, long resolved, long closed, long slaViolations,
            Double slaCompliancePercent, Double mttaMinutes, Double mttrMinutes) {
    }

    public record TechnicianWorkload(long id, String name, String role, long active, int maxLoad,
            boolean available, long resolvedLast7Days, Double avgResolutionMinutes) {
    }

    /** health: OUTAGE (an open P1), DEGRADED (anything open) or HEALTHY. */
    public record ServiceAnalysis(long id, String name, String criticality, long incidents, long open,
            String health, List<String> topRootCauses) {
    }

    /** Incidents per day per category; every day in the window is present, even with no incidents. */
    public record Trend(List<String> categories, List<TrendDay> days) {
    }

    public record TrendDay(LocalDate date, Map<String, Long> counts, long total) {
    }

    /** Resolution SLA per priority: met, breached (resolved late or still open past due) and open on track. */
    public record SlaByPriority(String priority, long met, long breached, long openOnTrack) {
    }

    /**
     * suggestionAcceptancePercent: share of classified incidents where the reporter kept the suggested
     * category. ticketsAvoided: people who joined an existing incident plus duplicates merged into one.
     */
    public record AiEffectiveness(Double suggestionAcceptancePercent, long classified, long duplicatesJoined,
            long duplicatesMerged, long ticketsAvoided, long majorIncidents) {
    }
}

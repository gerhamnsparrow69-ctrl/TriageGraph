package in.ac.bitspilani.triagegraph.sla.application;

public record SlaBreachReport(
        long responseBreachCount,
        long resolutionBreachCount,
        long incidentsWithBreaches) {
}

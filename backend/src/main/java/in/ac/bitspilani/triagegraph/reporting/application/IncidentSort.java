package in.ac.bitspilani.triagegraph.reporting.application;

import java.util.Comparator;
import java.util.Locale;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/** The orders a search result can be sorted in. Each constant carries its own comparator. */
public enum IncidentSort {

    NEWEST(Comparator.comparing(IncidentFact::createdAt).reversed()),
    OLDEST(Comparator.comparing(IncidentFact::createdAt)),
    PRIORITY(Comparator.comparing((IncidentFact i) -> i.priority() == null ? 9 : i.priority().ordinal())
            .thenComparing(Comparator.comparing(IncidentFact::createdAt).reversed())),
    DUE_SOONEST(Comparator.comparing(IncidentFact::resolutionDueAt, Comparator.nullsLast(Comparator.naturalOrder()))),
    STATUS(Comparator.comparing(IncidentFact::status).thenComparing(IncidentFact::createdAt, Comparator.reverseOrder()));

    private final Comparator<IncidentFact> comparator;

    IncidentSort(Comparator<IncidentFact> comparator) {
        this.comparator = comparator;
    }

    public Comparator<IncidentFact> comparator() {
        return comparator;
    }

    /** Accepts "newest", "due-soonest" or "DUE_SOONEST"; blank means NEWEST. */
    public static IncidentSort parse(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown sort '" + value + "'.");
        }
    }
}

package in.ac.bitspilani.triagegraph.reporting.domain;

/**
 * Specification pattern (blueprint §10.3): one search rule. Rules combine with {@link #and}, so a search with
 * eleven optional criteria is eleven small objects instead of one method full of if/else.
 */
@FunctionalInterface
public interface IncidentSpecification {

    boolean isSatisfiedBy(IncidentFact incident);

    default IncidentSpecification and(IncidentSpecification other) {
        return incident -> isSatisfiedBy(incident) && other.isSatisfiedBy(incident);
    }

    static IncidentSpecification any() {
        return incident -> true;
    }
}

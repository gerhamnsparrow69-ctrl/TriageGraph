package in.ac.bitspilani.triagegraph.reporting.domain;

/** An IT service from the catalogue, for the service analysis report. */
public record ServiceFact(long id, String name, String criticality) {
}

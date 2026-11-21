package in.ac.bitspilani.triagegraph.reporting.domain;

/** A technician or team lead as the workload report needs it. */
public record TechnicianFact(long id, String name, String role, int maxLoad, boolean available, Long teamLeadId) {
}

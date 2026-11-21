package in.ac.bitspilani.triagegraph.reporting.application;

import java.util.List;
import java.util.Set;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact;
import in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact;

/**
 * Read-only view of the data reports need (port; the adapter is JdbcIncidentReadModel). Reports read rows,
 * never aggregates, so reporting cannot change an incident by mistake.
 */
public interface IncidentReadModel {

    List<IncidentFact> incidents();

    List<TechnicianFact> technicians();

    List<ServiceFact> services();

    /** Codes of the active categories, for the search filters. */
    List<String> categories();

    /** Ids of the technicians whose team lead is {@code teamLeadId}. */
    Set<Long> teamMembers(long teamLeadId);
}

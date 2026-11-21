package in.ac.bitspilani.triagegraph.reporting.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact;
import in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact;

/** In-memory read model for the service tests. */
class FakeReadModel implements IncidentReadModel {

    final List<IncidentFact> incidents = new ArrayList<>();
    final List<TechnicianFact> technicians = new ArrayList<>();
    final List<ServiceFact> services = new ArrayList<>();
    Map<Long, Set<Long>> teams = Map.of();

    @Override
    public List<IncidentFact> incidents() {
        return incidents;
    }

    @Override
    public List<TechnicianFact> technicians() {
        return technicians;
    }

    @Override
    public List<ServiceFact> services() {
        return services;
    }

    @Override
    public List<String> categories() {
        return List.of("NETWORK", "SOFTWARE");
    }

    @Override
    public Set<Long> teamMembers(long teamLeadId) {
        return teams.getOrDefault(teamLeadId, Set.of());
    }
}

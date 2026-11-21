package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.List;

public interface TechnicianFixRepository {

    boolean existsByIncidentCode(String incidentCode);

    TechnicianFix save(TechnicianFix fix);

    List<TechnicianFix> findAll();
}

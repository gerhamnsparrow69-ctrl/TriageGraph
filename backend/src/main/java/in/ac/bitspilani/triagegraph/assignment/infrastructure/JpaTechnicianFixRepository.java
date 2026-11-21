package in.ac.bitspilani.triagegraph.assignment.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFix;
import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFixRepository;

@Repository
class JpaTechnicianFixRepository implements TechnicianFixRepository {

    private final SpringDataTechnicianFixRepository jpa;

    JpaTechnicianFixRepository(SpringDataTechnicianFixRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByIncidentCode(String incidentCode) {
        return jpa.existsByIncidentCode(incidentCode);
    }

    @Override
    public TechnicianFix save(TechnicianFix fix) {
        return jpa.save(fix);
    }

    @Override
    public List<TechnicianFix> findAll() {
        return jpa.findAll();
    }
}

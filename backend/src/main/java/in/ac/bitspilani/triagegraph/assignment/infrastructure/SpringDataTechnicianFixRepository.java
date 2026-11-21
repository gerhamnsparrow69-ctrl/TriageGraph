package in.ac.bitspilani.triagegraph.assignment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFix;

interface SpringDataTechnicianFixRepository extends JpaRepository<TechnicianFix, Long> {

    boolean existsByIncidentCode(String incidentCode);
}

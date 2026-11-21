package in.ac.bitspilani.triagegraph.catalog.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.catalog.domain.ChangeRecord;

public interface ChangeRecordRepository extends JpaRepository<ChangeRecord, Long> {

    List<ChangeRecord> findByServiceIdOrderByLoggedAtDesc(Long serviceId);
}

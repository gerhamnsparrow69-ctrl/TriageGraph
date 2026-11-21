package in.ac.bitspilani.triagegraph.catalog.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.ac.bitspilani.triagegraph.catalog.domain.ChangeRecord;

public interface ChangeRecordRepository extends JpaRepository<ChangeRecord, Long> {

    List<ChangeRecord> findByServiceIdOrderByLoggedAtDesc(Long serviceId);

    @Query("""
            select change from ChangeRecord change
            where change.service.id in :serviceIds
              and change.loggedAt >= :fromInclusive
              and change.loggedAt <= :toInclusive
            order by change.loggedAt desc, change.id asc
            """)
    List<ChangeRecord> findRecentChanges(
            @Param("serviceIds") Set<Long> serviceIds,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toInclusive") Instant toInclusive);
}

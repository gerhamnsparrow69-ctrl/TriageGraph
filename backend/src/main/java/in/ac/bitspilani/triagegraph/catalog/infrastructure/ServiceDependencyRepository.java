package in.ac.bitspilani.triagegraph.catalog.infrastructure;

import in.ac.bitspilani.triagegraph.catalog.domain.ServiceDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceDependencyRepository
        extends JpaRepository<ServiceDependency, Long> {

    List<ServiceDependency> findByDependentId(Long serviceId);

    List<ServiceDependency> findByDependencyId(Long serviceId);
}
package in.ac.bitspilani.triagegraph.catalog.infrastructure;

import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    Optional<ITService> findByCode(String code);
}
package in.ac.bitspilani.triagegraph.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;

interface SpringDataUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select t from Technician t order by t.name")
    List<Technician> findAllTechnicians();
}

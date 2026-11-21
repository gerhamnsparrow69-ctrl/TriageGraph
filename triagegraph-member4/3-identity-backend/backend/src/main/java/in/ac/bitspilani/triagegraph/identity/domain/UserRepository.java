package in.ac.bitspilani.triagegraph.identity.domain;

import java.util.List;
import java.util.Optional;

/** Domain view of user storage; the Spring Data implementation lives in infrastructure. */
public interface UserRepository {

    Optional<User> findById(long id);

    Optional<User> findByEmail(String normalisedEmail);

    boolean existsByEmail(String normalisedEmail);

    List<User> findAll();

    List<Technician> findAllTechnicians();

    User save(User user);
}

package in.ac.bitspilani.triagegraph.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;

@Repository
class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository jpa;

    JpaUserRepository(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String normalisedEmail) {
        return jpa.findByEmail(normalisedEmail);
    }

    @Override
    public boolean existsByEmail(String normalisedEmail) {
        return jpa.existsByEmail(normalisedEmail);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll(Sort.by("name"));
    }

    @Override
    public List<Technician> findAllTechnicians() {
        return jpa.findAllTechnicians();
    }

    @Override
    public User save(User user) {
        return jpa.save(user);
    }
}

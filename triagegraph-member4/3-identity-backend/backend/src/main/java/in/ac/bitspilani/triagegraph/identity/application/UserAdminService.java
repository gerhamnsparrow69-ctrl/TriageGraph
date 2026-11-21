package in.ac.bitspilani.triagegraph.identity.application;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.identity.domain.TeamLead;
import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

/** Admin-side user and technician management. Authorisation is enforced on the controller. */
@Service
@Transactional
public class UserAdminService {

    private final UserRepository users;
    private final UserFactory factory;

    public UserAdminService(UserRepository users, UserFactory factory) {
        this.users = users;
        this.factory = factory;
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAll();
    }

    @Transactional(readOnly = true)
    public List<Technician> technicians() {
        return users.findAllTechnicians();
    }

    public User create(NewUser newUser) {
        String email = User.normaliseEmail(newUser.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        return users.save(factory.create(newUser));
    }

    public User updateProfile(long id, String name, String department, String pushTopic) {
        User user = load(id);
        user.updateProfile(name, department, pushTopic);
        return user;
    }

    public User setActive(long id, boolean active) {
        User user = load(id);
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return user;
    }

    public Technician updateTechnician(long id, Set<String> skills, int experienceYears, int maxLoad,
            boolean available, Long teamLeadId) {
        if (!(load(id) instanceof Technician tech)) {
            throw new InvalidInputException("User " + id + " is not a technician.");
        }
        tech.changeSkills(skills);
        tech.changeCapacity(experienceYears, maxLoad);
        tech.markAvailable(available);
        tech.reportTo(teamLeadId == null ? null : teamLead(teamLeadId));
        return tech;
    }

    private TeamLead teamLead(long id) {
        if (load(id) instanceof TeamLead lead) {
            return lead;
        }
        throw new InvalidInputException("User " + id + " is not a team lead.");
    }

    private User load(long id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException("No user with id " + id));
    }
}

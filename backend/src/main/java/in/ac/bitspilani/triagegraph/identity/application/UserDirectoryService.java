package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

/**
 * Implements the {@link UserDirectory} port for other modules. Live load comes from Member 1's
 * {@link IncidentQueryPort} when it exists; until then everyone has a load of 0.
 */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectory {

    private final UserRepository users;
    private final ObjectProvider<IncidentQueryPort> incidents;

    public UserDirectoryService(UserRepository users, ObjectProvider<IncidentQueryPort> incidents) {
        this.users = users;
        this.incidents = incidents;
    }

    @Override
    public UserRef user(long id) {
        return users.findById(id).map(User::toRef)
                .orElseThrow(() -> new UserNotFoundException("No user with id " + id));
    }

    @Override
    public List<TechnicianProfile> technicians() {
        Map<Long, Long> load = activeLoadByTechnician();
        // fixesByService needs resolved-incident history (Member 1/2); empty until that port exists.
        return users.findAllTechnicians().stream()
                .map(t -> t.toProfile(load.getOrDefault(t.getId(), 0L).intValue(), Map.of()))
                .toList();
    }

    @Override
    public Optional<UserRef> teamLeadOf(long technicianId) {
        return users.findById(technicianId)
                .filter(Technician.class::isInstance)
                .map(u -> ((Technician) u).getTeamLead())
                .map(User::toRef);
    }

    @Override
    public List<UserRef> withRole(Role role) {
        return users.findAll().stream()
                .filter(u -> u.role() == role && u.isActive())
                .map(User::toRef)
                .toList();
    }

    private Map<Long, Long> activeLoadByTechnician() {
        IncidentQueryPort port = incidents.getIfAvailable();
        if (port == null) {
            return Map.of();
        }
        return port.openSince(Instant.EPOCH).stream()
                .map(IncidentSnapshot::assigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}

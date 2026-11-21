package in.ac.bitspilani.triagegraph.assignment.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettings;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettingsRepository;
import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFix;
import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFixRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentCommandPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

/** In-memory stand-ins for the ports and repositories the assignment module depends on. */
final class Fakes {

    private Fakes() {
    }

    static final class Directory implements UserDirectory {
        final List<TechnicianProfile> technicians = new ArrayList<>();
        final Map<Long, Long> leadOf = new HashMap<>();
        final Map<Long, UserRef> users = new HashMap<>();

        @Override
        public UserRef user(long id) {
            UserRef u = users.get(id);
            if (u == null) {
                throw new UserNotFoundException("No user with id " + id);
            }
            return u;
        }

        @Override
        public List<TechnicianProfile> technicians() {
            return List.copyOf(technicians);
        }

        @Override
        public Optional<UserRef> teamLeadOf(long technicianId) {
            return Optional.ofNullable(leadOf.get(technicianId)).map(id -> new UserRef(id, "Lead " + id, Role.TEAM_LEAD, null, null));
        }

        @Override
        public List<UserRef> withRole(Role role) {
            return users.values().stream().filter(u -> u.role() == role).toList();
        }
    }

    static final class SettingsRepo implements AssignmentSettingsRepository {
        AssignmentSettings stored;

        @Override
        public Optional<AssignmentSettings> find() {
            return Optional.ofNullable(stored);
        }

        @Override
        public AssignmentSettings save(AssignmentSettings settings) {
            stored = settings;
            return settings;
        }
    }

    static final class FixRepo implements TechnicianFixRepository {
        final List<TechnicianFix> rows = new ArrayList<>();

        @Override
        public boolean existsByIncidentCode(String code) {
            return rows.stream().anyMatch(r -> r.getIncidentCode().equals(code));
        }

        @Override
        public TechnicianFix save(TechnicianFix fix) {
            rows.add(fix);
            return fix;
        }

        @Override
        public List<TechnicianFix> findAll() {
            return List.copyOf(rows);
        }
    }

    static final class Incidents implements IncidentQueryPort {
        final List<IncidentSnapshot> open = new ArrayList<>();

        @Override
        public Optional<IncidentSnapshot> find(long id) {
            return open.stream().filter(s -> s.id() == id).findFirst();
        }

        @Override
        public List<IncidentSnapshot> openSince(Instant since) {
            return List.copyOf(open);
        }

        @Override
        public List<IncidentSnapshot> openWithDeadlineBefore(Instant t) {
            return List.of();
        }
    }

    static final class Commands implements IncidentCommandPort {
        final List<String> calls = new ArrayList<>();

        @Override
        public void markSlaWarning(long incidentId, SlaKind kind) {
            calls.add("warning " + incidentId);
        }

        @Override
        public void markSlaBreached(long incidentId, SlaKind kind) {
            calls.add("breached " + incidentId);
        }

        @Override
        public void escalate(long incidentId, int level, long targetUserId, String reason) {
            calls.add("escalate " + incidentId);
        }

        @Override
        public void reassign(long incidentId, long technicianId, String reason, long actorId) {
            calls.add("reassign " + incidentId + " to " + technicianId + " by " + actorId + ": " + reason);
        }

        @Override
        public void joinAsDuplicate(long incidentId, long userId) {
            calls.add("join " + incidentId + " user " + userId);
        }

        @Override
        public void merge(long childId, long parentId, long actorId) {
            calls.add("merge " + childId + " into " + parentId + " by " + actorId);
        }
    }
}

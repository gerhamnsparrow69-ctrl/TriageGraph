package in.ac.bitspilani.triagegraph.triage.application;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentCommandPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;

/**
 * The two actions behind a duplicate: an employee joins an existing incident instead of filing a new one
 * (ticket deflection), or a technician merges a duplicate into its parent. Checks happen here; the change
 * itself is Member 1's {@link IncidentCommandPort}, which also recalculates priority for the extra user.
 */
@Service
public class JoinMergeService {

    private static final Set<IncidentStatus> FINISHED = EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
            IncidentStatus.CLOSED, IncidentStatus.MERGED);

    private final ObjectProvider<IncidentCommandPort> commands;
    private final ObjectProvider<IncidentQueryPort> incidents;

    public JoinMergeService(ObjectProvider<IncidentCommandPort> commands, ObjectProvider<IncidentQueryPort> incidents) {
        this.commands = commands;
        this.incidents = incidents;
    }

    public void join(long incidentId, long userId) {
        IncidentCommandPort port = commandPort();
        snapshot(incidentId).ifPresent(s -> {
            requireOpen(s, "join");
            if (s.reporterId() == userId || (s.followerIds() != null && s.followerIds().contains(userId))) {
                throw new InvalidInputException("You are already following " + s.code() + ".");
            }
        });
        port.joinAsDuplicate(incidentId, userId);
    }

    public void merge(long childId, long parentId, long actorId) {
        if (childId == parentId) {
            throw new InvalidInputException("An incident cannot be merged into itself.");
        }
        IncidentCommandPort port = commandPort();
        snapshot(childId).ifPresent(s -> requireOpen(s, "merge"));
        snapshot(parentId).ifPresent(s -> requireOpen(s, "merge into"));
        port.merge(childId, parentId, actorId);
    }

    private Optional<IncidentSnapshot> snapshot(long id) {
        IncidentQueryPort port = incidents.getIfAvailable();
        if (port == null) {
            return Optional.empty();
        }
        return Optional.of(port.find(id).orElseThrow(() -> new IncidentNotFoundException("No incident with id " + id)));
    }

    private static void requireOpen(IncidentSnapshot s, String action) {
        if (FINISHED.contains(s.status())) {
            throw new InvalidStateTransitionException("Cannot " + action + " " + s.code() + ": it is already "
                    + s.status() + ". Please report a new incident instead.");
        }
    }

    private IncidentCommandPort commandPort() {
        IncidentCommandPort port = commands.getIfAvailable();
        if (port == null) {
            throw new TriageDependencyUnavailableException("Joining and merging need the incident module, which is not deployed yet.");
        }
        return port;
    }
}

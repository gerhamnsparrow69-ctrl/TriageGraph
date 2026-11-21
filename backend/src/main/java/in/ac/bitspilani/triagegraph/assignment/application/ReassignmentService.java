package in.ac.bitspilani.triagegraph.assignment.application;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentCommandPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

/**
 * Manual reassignment by a team lead or admin. Capacity rules still apply: a full or unavailable technician
 * is rejected unless an administrator explicitly overrides (the override is visible in Member 1's audit
 * trail through the reason text).
 */
@Service
public class ReassignmentService {

    private final UserDirectory directory;
    private final ObjectProvider<IncidentCommandPort> commands;

    public ReassignmentService(UserDirectory directory, ObjectProvider<IncidentCommandPort> commands) {
        this.directory = directory;
        this.commands = commands;
    }

    public void reassign(long incidentId, long technicianId, String reason, long actorId, boolean override) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("Please give a reason for the reassignment.");
        }
        TechnicianProfile target = directory.technicians().stream().filter(p -> p.id() == technicianId).findFirst()
                .orElseThrow(() -> new TechnicianUnavailableException("User " + technicianId + " is not a technician."));
        if (!override && !target.available()) {
            throw new TechnicianUnavailableException(target.name() + " is marked unavailable.");
        }
        if (!override && target.activeLoad() >= target.maxLoad()) {
            throw new WorkloadExceededException(target.name() + " is at maximum workload (" + target.activeLoad()
                    + "/" + target.maxLoad() + "). An administrator can override.");
        }
        IncidentCommandPort port = commands.getIfAvailable();
        if (port == null) {
            throw new AssignmentDependencyUnavailableException("Reassignment needs the incident module, which is not deployed yet.");
        }
        String auditReason = override ? "[admin override] " + reason.trim() : reason.trim();
        port.reassign(incidentId, technicianId, auditReason, actorId);
    }
}

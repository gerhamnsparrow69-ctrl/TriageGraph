package in.ac.bitspilani.triagegraph.assignment.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.assignment.domain.Workload;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

/** Technician workload and queues for the technician console and the team-lead view. */
@Service
public class WorkloadService {

    /** Statuses that no longer belong in anyone's queue. */
    private static final Set<IncidentStatus> FINISHED = EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.MERGED);
    /** Statuses where the technician is waiting on someone else. */
    private static final Set<IncidentStatus> WAITING = EnumSet.of(IncidentStatus.PENDING_INFO,
            IncidentStatus.RESOLVED, IncidentStatus.VERIFIED);

    public record QueueItem(long incidentId, String code, String title, IncidentStatus status, Priority priority,
            Instant createdAt, Instant responseDueAt, Instant resolutionDueAt) {
    }

    public record TechnicianWorkload(long technicianId, String name, Set<String> skills, int activeLoad, int maxLoad,
            int loadPercent, boolean available, boolean atCapacity, List<QueueItem> queue,
            boolean incidentDataAvailable) {
    }

    private final UserDirectory directory;
    private final ObjectProvider<IncidentQueryPort> incidents;

    public WorkloadService(UserDirectory directory, ObjectProvider<IncidentQueryPort> incidents) {
        this.directory = directory;
        this.incidents = incidents;
    }

    /** @throws UserNotFoundException the user is not a technician or team lead */
    public TechnicianWorkload forTechnician(long technicianId) {
        TechnicianProfile profile = directory.technicians().stream().filter(p -> p.id() == technicianId).findFirst()
                .orElseThrow(() -> new UserNotFoundException("User " + technicianId + " is not a technician."));
        return toWorkload(profile, queues());
    }

    /** Everyone who reports to the team lead, plus the lead (leads take incidents too). */
    public List<TechnicianWorkload> team(long teamLeadId) {
        Map<Long, List<QueueItem>> queues = queues();
        return directory.technicians().stream()
                .filter(p -> p.id() == teamLeadId || directory.teamLeadOf(p.id()).map(UserRef::id)
                        .filter(id -> id == teamLeadId).isPresent())
                .map(p -> toWorkload(p, queues))
                .sorted(Comparator.comparingInt(TechnicianWorkload::loadPercent).reversed())
                .toList();
    }

    public List<TechnicianWorkload> everyone() {
        Map<Long, List<QueueItem>> queues = queues();
        return directory.technicians().stream()
                .map(p -> toWorkload(p, queues))
                .sorted(Comparator.comparingInt(TechnicianWorkload::loadPercent).reversed())
                .toList();
    }

    private TechnicianWorkload toWorkload(TechnicianProfile p, Map<Long, List<QueueItem>> queues) {
        int percent = (int) Math.round(Workload.ratio(p) * 100);
        return new TechnicianWorkload(p.id(), p.name(), p.skills(), p.activeLoad(), p.maxLoad(), percent,
                p.available(), p.activeLoad() >= p.maxLoad(), queues.getOrDefault(p.id(), List.of()),
                incidents.getIfAvailable() != null);
    }

    /** Open incidents grouped by assignee, most urgent first. Empty until Member 1's incident module exists. */
    private Map<Long, List<QueueItem>> queues() {
        IncidentQueryPort port = incidents.getIfAvailable();
        if (port == null) {
            return Map.of();
        }
        return port.openSince(Instant.EPOCH).stream()
                .filter(s -> s.assigneeId() != null && !FINISHED.contains(s.status()))
                .sorted(URGENCY)
                .collect(Collectors.groupingBy(IncidentSnapshot::assigneeId,
                        Collectors.mapping(WorkloadService::toItem, Collectors.toList())));
    }

    /** Needs-action before waiting, then P1 before P4, then the earliest resolution deadline. */
    static final Comparator<IncidentSnapshot> URGENCY = Comparator
            .comparing((IncidentSnapshot s) -> WAITING.contains(s.status()))
            .thenComparing(s -> Optional.ofNullable(s.priority()).map(Enum::ordinal).orElse(99))
            .thenComparing(IncidentSnapshot::resolutionDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IncidentSnapshot::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private static QueueItem toItem(IncidentSnapshot s) {
        return new QueueItem(s.id(), s.code(), s.title(), s.status(), s.priority(), s.createdAt(),
                s.responseDueAt(), s.resolutionDueAt());
    }
}

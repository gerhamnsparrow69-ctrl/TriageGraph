package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.AssignmentService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

import java.util.Comparator;
import java.util.List;

/**
 * Picks the least-loaded available technician, ignoring skills. Replaced by M2's strategies (roadmap step 8).
 */
public final class StubAssignmentService implements AssignmentService {

    public static final String KEY = "STUB_LEAST_LOADED";

    private static final Comparator<TechnicianProfile> LEAST_LOADED =
            Comparator.comparingInt(TechnicianProfile::activeLoad).thenComparingLong(TechnicianProfile::id);

    private final UserDirectory users;

    public StubAssignmentService(UserDirectory users) {
        this.users = users;
    }

    @Override
    public TechnicianRecommendation autoAssign(AssignmentRequest req) {
        return recommend(req, 1).getFirst();
    }

    @Override
    public List<TechnicianRecommendation> recommend(AssignmentRequest req, int topK) {
        List<TechnicianProfile> available = users.technicians().stream().filter(TechnicianProfile::available).toList();
        if (available.isEmpty()) {
            throw new TechnicianUnavailableException("No technician is available.");
        }
        List<TechnicianProfile> withCapacity = available.stream().filter(t -> t.activeLoad() < t.maxLoad()).toList();
        if (withCapacity.isEmpty()) {
            throw new WorkloadExceededException("Every available technician is at full workload.");
        }
        return withCapacity.stream().sorted(LEAST_LOADED).limit(Math.max(1, topK))
                .map(t -> new TechnicianRecommendation(t.id(), t.name(), 1.0, KEY,
                        List.of("Load " + t.activeLoad() + "/" + t.maxLoad() + " (stub: least loaded)")))
                .toList();
    }
}

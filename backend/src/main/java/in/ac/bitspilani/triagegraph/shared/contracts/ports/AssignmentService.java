package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import java.util.List;

/** Provided by Member 2 (assignment). */
public interface AssignmentService {

    /**
     * @throws in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException nobody has the skill
     * @throws in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException everyone with the skill is full
     */
    TechnicianRecommendation autoAssign(AssignmentRequest req);

    List<TechnicianRecommendation> recommend(AssignmentRequest req, int topK);
}

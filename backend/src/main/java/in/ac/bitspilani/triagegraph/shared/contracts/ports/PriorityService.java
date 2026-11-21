package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;

/** Provided by Member 1 (priority). */
public interface PriorityService {

    PriorityAssessment assess(PriorityInput in);
}

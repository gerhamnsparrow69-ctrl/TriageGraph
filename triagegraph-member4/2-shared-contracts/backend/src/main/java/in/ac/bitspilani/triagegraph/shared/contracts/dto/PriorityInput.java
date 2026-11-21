package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;

public record PriorityInput(
        Impact impact, Urgency urgency, Criticality criticality,
        int affectedUsers, int blastRadius, String categoryCode) {
}

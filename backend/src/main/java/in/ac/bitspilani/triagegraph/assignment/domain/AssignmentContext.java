package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;

/**
 * What a strategy needs to rank technicians: the incident's assignment request plus the skill that the
 * incident's category requires (resolved once, before eligibility filtering).
 */
public record AssignmentContext(AssignmentRequest request, String requiredSkill) {

    public AssignmentContext {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(requiredSkill, "requiredSkill");
    }

    public boolean urgent() {
        return request.priority() != null && request.priority().ordinal() <= 1; // P1 or P2
    }
}

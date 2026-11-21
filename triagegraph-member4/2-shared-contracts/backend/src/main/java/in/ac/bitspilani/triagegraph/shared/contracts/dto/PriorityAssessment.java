package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

public record PriorityAssessment(
        Priority priority, double score, String calculatorKey, String rationale) {
}

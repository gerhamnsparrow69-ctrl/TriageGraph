package in.ac.bitspilani.triagegraph.assignment.api;

import java.time.Instant;
import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the assignment endpoints. */
public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    /** "Who should take an incident like this?" Service id 0 means unknown. */
    public record RecommendationQuery(
            @NotBlank @Size(max = 40) String categoryCode,
            Long serviceId,
            @NotNull Priority priority,
            @Min(1) @Max(10) Integer topK,
            @Size(max = 40) String strategyKey) {

        AssignmentRequest toRequest() {
            return new AssignmentRequest(0L, categoryCode.trim(), serviceId == null ? 0L : serviceId, priority);
        }

        int limit() {
            return topK == null ? 3 : topK;
        }
    }

    public record ReassignRequest(
            @NotNull Long technicianId,
            @NotBlank @Size(max = 500) String reason,
            boolean override) {
    }

    public record StrategyView(String key, String displayName, String description, boolean active) {
    }

    public record AssignmentSettingsView(String activeStrategy, List<StrategyView> strategies, Instant updatedAt,
            Long updatedBy) {
    }

    public record SwitchStrategyRequest(@NotBlank @Size(max = 40) String strategyKey) {
    }
}

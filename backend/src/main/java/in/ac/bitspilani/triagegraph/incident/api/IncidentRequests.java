package in.ac.bitspilani.triagegraph.incident.api;

import in.ac.bitspilani.triagegraph.incident.application.ReportIncidentCommand;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.InvestigationNote;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request bodies for the incident endpoints. */
final class IncidentRequests {

    private IncidentRequests() {
    }

    record Report(
            @NotBlank @Size(max = Incident.MAX_TITLE) String title,
            @NotBlank @Size(max = Incident.MAX_DESCRIPTION) String description,
            @NotNull Impact impact,
            @NotNull Urgency urgency,
            @Min(1) @Max(100_000) Integer affectedUsers,
            @Size(max = 50) String categoryCode,
            Long serviceId) {

        ReportIncidentCommand toCommand() {
            return new ReportIncidentCommand(title, description, impact, urgency,
                    affectedUsers == null ? 1 : affectedUsers, categoryCode, serviceId);
        }
    }

    record Assign(@NotNull Long technicianId) {
    }

    /** Question, answer or reopen reason. */
    record Message(@NotBlank @Size(max = 1000) String text) {
    }

    record Resolve(@NotBlank @Size(max = 2000) String summary, @Size(max = 1000) String rootCause) {
    }

    /** {@code internal} is optional (Jackson 3 rejects a missing primitive, so it is a Boolean). */
    record Note(@NotBlank @Size(max = InvestigationNote.MAX_LENGTH) String text, Boolean internal) {

        boolean isInternal() {
            return Boolean.TRUE.equals(internal);
        }
    }

    record Feedback(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }
}

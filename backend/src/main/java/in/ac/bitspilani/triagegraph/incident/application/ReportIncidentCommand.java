package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;

/**
 * What the reporter typed. {@code categoryCode} and {@code serviceId} are set when they kept (or changed)
 * the suggestion shown while typing; otherwise triage classifies the text.
 */
public record ReportIncidentCommand(String title, String description, Impact impact, Urgency urgency,
        int affectedUsers, String categoryCode, Long serviceId) {

    public boolean hasUserChoice() {
        return categoryCode != null && !categoryCode.isBlank() && serviceId != null;
    }

    /** The text triage classifies and indexes. */
    public String text() {
        return title + "\n" + description;
    }

    ClassificationSuggestion userChoice() {
        return new ClassificationSuggestion(categoryCode.strip().toUpperCase(), serviceId, 1.0, "USER",
                "Chosen by the reporter");
    }
}

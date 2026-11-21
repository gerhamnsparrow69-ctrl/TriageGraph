package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageResult;
import java.util.List;

/** Provided by Member 2 (triage). */
public interface TriageService {

    TriageResult preview(TriageRequest req);

    ClassificationSuggestion classify(String text);

    void index(long incidentId, String text);

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException on a near-identical repeat */
    void checkNotRepeatSubmission(long reporterId, String text);

    List<SimilarIncident> similarResolved(long incidentId, int k);

    ResolutionSuggestion suggestResolution(long incidentId, boolean useAi);
}

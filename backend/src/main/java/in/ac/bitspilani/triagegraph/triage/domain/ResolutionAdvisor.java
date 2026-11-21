package in.ac.bitspilani.triagegraph.triage.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;

/** Intelligent Feature G: suggests how to fix an incident from how similar incidents were fixed. */
public interface ResolutionAdvisor {

    ResolutionSuggestion advise(ResolutionQuery query);
}

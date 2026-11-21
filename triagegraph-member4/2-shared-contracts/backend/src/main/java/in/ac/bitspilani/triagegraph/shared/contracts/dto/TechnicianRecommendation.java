package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.List;

public record TechnicianRecommendation(
        long technicianId, String name, double score,
        String strategyKey, List<String> reasons) {
}

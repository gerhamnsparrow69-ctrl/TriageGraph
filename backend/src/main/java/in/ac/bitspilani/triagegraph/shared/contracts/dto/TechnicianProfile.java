package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.Map;
import java.util.Set;

public record TechnicianProfile(
        long id, String name, Set<String> skills, int activeLoad, int maxLoad,
        int experienceYears, Map<Long, Integer> fixesByService, boolean available) {
}

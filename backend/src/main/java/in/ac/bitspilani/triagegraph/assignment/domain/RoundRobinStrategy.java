package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;

/**
 * Takes turns within each category: the next eligible technician (by id) after the one assigned last time.
 * The cursor is the last assigned technician id per category, so it keeps working when someone becomes
 * unavailable or a new technician joins. Ranking alone never moves the cursor; only {@link #onAssigned} does.
 */
public class RoundRobinStrategy implements AssignmentStrategy {

    public static final String KEY = "ROUND_ROBIN";

    private final Map<String, Long> lastAssignedByCategory = new ConcurrentHashMap<>();

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Round robin";
    }

    @Override
    public String description() {
        return "Takes turns: each new incident in a category goes to the next skilled technician in a fixed order.";
    }

    @Override
    public List<TechnicianRecommendation> rank(AssignmentContext context, List<TechnicianProfile> eligible) {
        List<TechnicianProfile> ordered = eligible.stream().sorted(Comparator.comparingLong(TechnicianProfile::id)).toList();
        Long last = lastAssignedByCategory.get(cursorKey(context));
        int start = 0; // nobody assigned yet, or everyone's id <= last: start from the lowest id (wrap around)
        if (last != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).id() > last) {
                    start = i;
                    break;
                }
            }
        }
        int n = ordered.size();
        List<TechnicianRecommendation> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            TechnicianProfile t = ordered.get((start + i) % n);
            double score = n == 1 ? 1.0 : 1.0 - (double) i / n;
            String turn = i == 0 ? "next in rotation for " + context.requiredSkill() : "position " + (i + 1) + " in rotation";
            result.add(new TechnicianRecommendation(t.id(), t.name(), Workload.round3(score), KEY,
                    List.of(turn, Workload.describe(t))));
        }
        return List.copyOf(result);
    }

    @Override
    public void onAssigned(AssignmentContext context, long technicianId) {
        lastAssignedByCategory.put(cursorKey(context), technicianId);
    }

    private static String cursorKey(AssignmentContext context) {
        String category = context.request().categoryCode();
        return category == null ? "" : category.toUpperCase(Locale.ROOT);
    }
}

package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;

/** Spreads work evenly: the eligible technician with the most free capacity (lowest load ratio) ranks first. */
public class LoadBalancedStrategy extends AbstractScoringStrategy {

    public static final String KEY = "LOAD_BALANCED";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Load-balanced";
    }

    @Override
    public String description() {
        return "Picks the skilled technician with the most free capacity, so work is spread evenly.";
    }

    @Override
    protected Score score(AssignmentContext context, TechnicianProfile t) {
        int free = t.maxLoad() - t.activeLoad();
        return new Score(Workload.headroom(t), List.of(context.requiredSkill() + " skill", Workload.describe(t),
                free + (free == 1 ? " free slot" : " free slots")));
    }
}

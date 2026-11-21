package in.ac.bitspilani.triagegraph.assignment.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;

/** Small helpers for reading a technician's workload. */
public final class Workload {

    private Workload() {
    }

    /** Active load as a fraction of max load, 0.0 (idle) to 1.0 (full). */
    public static double ratio(TechnicianProfile p) {
        if (p.maxLoad() <= 0) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) p.activeLoad() / p.maxLoad()));
    }

    /** Free capacity as a fraction, 1.0 (idle) to 0.0 (full). */
    public static double headroom(TechnicianProfile p) {
        return 1.0 - ratio(p);
    }

    public static String describe(TechnicianProfile p) {
        return "load " + p.activeLoad() + "/" + p.maxLoad();
    }

    public static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}

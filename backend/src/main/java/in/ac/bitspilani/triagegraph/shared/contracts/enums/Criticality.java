package in.ac.bitspilani.triagegraph.shared.contracts.enums;

/** How important a service is to the campus. The weight feeds the priority score. */
public enum Criticality {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    MISSION_CRITICAL(4);

    private final int weight;

    Criticality(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}

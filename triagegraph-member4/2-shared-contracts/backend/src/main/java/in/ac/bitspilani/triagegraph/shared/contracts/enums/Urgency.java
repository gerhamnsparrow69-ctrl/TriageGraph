package in.ac.bitspilani.triagegraph.shared.contracts.enums;

/** How quickly the business needs a fix. The weight feeds the priority score. */
public enum Urgency {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    Urgency(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}

package in.ac.bitspilani.triagegraph.shared.contracts.enums;

/** How widely the incident affects the business. The weight feeds the priority score. */
public enum Impact {
    MINOR(1),
    MODERATE(2),
    SIGNIFICANT(3),
    EXTENSIVE(4);

    private final int weight;

    Impact(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}

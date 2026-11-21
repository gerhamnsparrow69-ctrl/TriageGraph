package in.ac.bitspilani.triagegraph.triage.domain;

/**
 * One admin-editable phrase. CATEGORY rules point at a category code ("APPLICATION"); SERVICE rules point at a
 * service name ("Fee Payment App") and act as aliases ("fee portal", "upi").
 */
public record KeywordRule(Long id, Target target, String key, String phrase) {

    public enum Target {
        CATEGORY, SERVICE
    }
}

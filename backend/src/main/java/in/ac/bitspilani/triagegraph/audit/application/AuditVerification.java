package in.ac.bitspilani.triagegraph.audit.application;

/** Result of walking the chain: either every entry checks out, or the first broken one is named. */
public record AuditVerification(boolean valid, long entriesChecked, Long firstBrokenId, String reason,
        String message) {

    static AuditVerification ok(long checked) {
        return new AuditVerification(true, checked, null, null, checked + " entries verified ✓");
    }

    static AuditVerification broken(long checked, long id, String reason) {
        return new AuditVerification(false, checked, id, reason,
                "Audit entry " + id + " failed verification: " + reason);
    }
}

package in.ac.bitspilani.triagegraph.audit.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditHasherTest {

    static final Instant T = Instant.parse("2026-11-20T09:00:00.123456Z");

    @Test
    void isDeterministicHexSha256() {
        String a = AuditHasher.hash(AuditHasher.GENESIS, 1L, 5L, AuditAction.CREATED, null, IncidentStatus.NEW, "Reported", T);

        assertThat(a).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(AuditHasher.hash(AuditHasher.GENESIS, 1L, 5L, AuditAction.CREATED, null, IncidentStatus.NEW,
                "Reported", T)).isEqualTo(a);
    }

    @Test
    void everyFieldChangesTheHash() {
        String base = AuditHasher.hash("p", 1L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T);

        assertThat(AuditHasher.hash("q", 1L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 2L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 6L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 5L, AuditAction.VERIFIED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 5L, AuditAction.CLOSED, IncidentStatus.RESOLVED, IncidentStatus.CLOSED, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, null, "d", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "e", T)).isNotEqualTo(base);
        assertThat(AuditHasher.hash("p", 1L, 5L, AuditAction.CLOSED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, "d", T.plusNanos(1000))).isNotEqualTo(base);
    }

    @Test
    void fieldBoundariesCannotBeShifted() {
        assertThat(AuditHasher.hash("p", 1L, 23L, AuditAction.CREATED, null, null, "x", T))
                .isNotEqualTo(AuditHasher.hash("p", 12L, 3L, AuditAction.CREATED, null, null, "x", T));
    }

    @Test
    void entriesAreStoredAtMicrosecondPrecisionSoTheyStillVerifyAfterTheDatabase() {
        AuditDraft draft = new AuditDraft(T.plusNanos(789), 5L, null, AuditAction.CREATED, 1L, "INC-1001",
                null, IncidentStatus.NEW, "Reported");

        AuditEntry e = AuditEntry.chain(draft, AuditHasher.GENESIS);

        assertThat(e.getOccurredAt()).isEqualTo(T);
        assertThat(e.computeHash()).isEqualTo(e.getHash());
    }

    @Test
    void editingAStoredFieldIsDetected() {
        AuditEntry e = AuditEntry.chain(new AuditDraft(T, 5L, null, AuditAction.RESOLVED, 1L, "INC-1001",
                IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, "Fixed"), AuditHasher.GENESIS);

        ReflectionTestUtils.setField(e, "description", "Fixed quickly");

        assertThat(e.computeHash()).isNotEqualTo(e.getHash());
    }

    @Test
    void longDescriptionsAreTruncated() {
        AuditEntry e = AuditEntry.chain(new AuditDraft(T, null, null, AuditAction.NOTE_ADDED, 1L, "INC-1001",
                null, null, "x".repeat(5000)), AuditHasher.GENESIS);

        assertThat(e.getDescription()).hasSize(AuditEntry.MAX_DESCRIPTION).endsWith("…");
        assertThat(e.computeHash()).isEqualTo(e.getHash());
    }
}

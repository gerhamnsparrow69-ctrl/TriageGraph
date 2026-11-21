package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditDraft;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.audit.domain.AuditHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditChainWriterTest {

    final InMemoryAuditRepository repo = new InMemoryAuditRepository();
    final AuditChainWriter writer = new AuditChainWriter(repo);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void firstEntryLinksToGenesisAndEachNextToThePreviousHash() {
        AuditEntry a = writer.append(draft("first"));
        AuditEntry b = writer.append(draft("second"));
        AuditEntry c = writer.append(draft("third"));

        assertThat(a.getPrevHash()).isEqualTo(AuditHasher.GENESIS);
        assertThat(b.getPrevHash()).isEqualTo(a.getHash());
        assertThat(c.getPrevHash()).isEqualTo(b.getHash());
        assertThat(writer.isChainLocked()).isFalse();
    }

    @Test
    void insideATransactionTheLockIsHeldUntilItCompletes() {
        TransactionSynchronizationManager.initSynchronization();

        writer.append(draft("one"));
        writer.append(draft("two"));
        assertThat(writer.isChainLocked()).as("held until commit or rollback").isTrue();

        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        assertThat(writer.isChainLocked()).isFalse();
    }

    private static AuditDraft draft(String description) {
        return new AuditDraft(Instant.parse("2026-11-20T09:00:00Z"), 1L, null, AuditAction.NOTE_ADDED, 7L,
                "INC-1007", null, null, description);
    }
}

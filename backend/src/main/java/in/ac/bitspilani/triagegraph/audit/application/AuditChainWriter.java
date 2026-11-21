package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditDraft;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.audit.domain.AuditHasher;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The only class that writes audit entries. It reads the newest hash and appends the next link under
 * one lock, so the chain order is deterministic.
 * <p>
 * Inside a transaction the lock is held until that transaction completes, not just until the insert:
 * otherwise a second transaction could read the same "newest" hash before the first commits, and the
 * chain would fork. The lock is reentrant, so one transaction can append many entries.
 * (One JVM is enough for this deployment; several instances would need a database lock instead.)
 */
@Component
public class AuditChainWriter {

    private final AuditRepository repository;
    private final ReentrantLock lock = new ReentrantLock(true);

    public AuditChainWriter(AuditRepository repository) {
        this.repository = repository;
    }

    public AuditEntry append(AuditDraft draft) {
        lock.lock();
        boolean releaseOnCompletion = false;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.unlock();
                    }
                });
                releaseOnCompletion = true;
            }
            String prevHash = repository.latest().map(AuditEntry::getHash).orElse(AuditHasher.GENESIS);
            return repository.append(AuditEntry.chain(draft, prevHash));
        } finally {
            if (!releaseOnCompletion) {
                lock.unlock();
            }
        }
    }

    /** For tests: whether some thread currently holds the chain lock. */
    boolean isChainLocked() {
        return lock.isLocked();
    }
}

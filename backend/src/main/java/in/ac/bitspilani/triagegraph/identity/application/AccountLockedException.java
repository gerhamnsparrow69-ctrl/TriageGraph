package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Instant;

import in.ac.bitspilani.triagegraph.shared.exception.AuthenticationFailedException;

/** Too many wrong passwords; mapped to 423 Locked by the identity API. */
public class AccountLockedException extends AuthenticationFailedException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Too many failed attempts. Try again after " + lockedUntil + ".");
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}

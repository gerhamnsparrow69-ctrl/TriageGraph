package in.ac.bitspilani.triagegraph.identity.api;

import java.time.Clock;
import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import in.ac.bitspilani.triagegraph.identity.application.AccountLockedException;

/** 423 Locked for the login lockout; runs before the global handler, which would answer 401. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentityExceptionHandler {

    private final Clock clock;

    IdentityExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ProblemDetail> locked(AccountLockedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, ex.getMessage());
        pd.setTitle("Account locked");
        pd.setProperty("errorCode", "ACCOUNT_LOCKED");
        pd.setProperty("lockedUntil", ex.lockedUntil());
        pd.setProperty("timestamp", Instant.now(clock));
        return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
    }
}

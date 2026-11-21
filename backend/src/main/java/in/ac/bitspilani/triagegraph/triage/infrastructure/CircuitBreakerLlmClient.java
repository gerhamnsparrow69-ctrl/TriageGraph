package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

/**
 * Decorator: after {@code failureThreshold} failures in a row the circuit opens and calls are skipped for
 * {@code openDuration} (answering "no opinion" instantly instead of waiting for timeouts). Then one trial
 * call is let through: success closes the circuit, failure opens it again. Failures never reach the caller.
 */
public class CircuitBreakerLlmClient implements LlmClient {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerLlmClient.class);

    private final LlmClient delegate;
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openUntil = Instant.MIN;
    private long skipped;

    public CircuitBreakerLlmClient(LlmClient delegate, int failureThreshold, Duration openDuration, Clock clock) {
        this.delegate = delegate;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDuration = openDuration;
        this.clock = clock;
    }

    @Override
    public Optional<String> generateText(LlmPrompt prompt) {
        return guarded(() -> delegate.generateText(prompt));
    }

    @Override
    public Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> responseSchema) {
        return guarded(() -> delegate.generateJson(prompt, responseSchema));
    }

    private <T> Optional<T> guarded(Supplier<Optional<T>> call) {
        if (!allowCall()) {
            return Optional.empty();
        }
        try {
            Optional<T> result = call.get();
            onSuccess();
            return result;
        } catch (RuntimeException e) {
            onFailure(e);
            return Optional.empty();
        }
    }

    private synchronized boolean allowCall() {
        if (state == State.OPEN) {
            if (clock.instant().isBefore(openUntil)) {
                skipped++;
                return false;
            }
            state = State.HALF_OPEN;
        }
        return true;
    }

    private synchronized void onSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
    }

    private synchronized void onFailure(RuntimeException e) {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openUntil = clock.instant().plus(openDuration);
            log.warn("LLM circuit opened for {} after {} failure(s): {}", openDuration, consecutiveFailures, e.getMessage());
        } else {
            log.warn("LLM call failed ({} of {}): {}", consecutiveFailures, failureThreshold, e.getMessage());
        }
    }

    public synchronized State state() {
        return state;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public synchronized String status() {
        return delegate.status() + "; circuit " + state + " (" + consecutiveFailures + " recent failures, "
                + skipped + " calls skipped)";
    }
}

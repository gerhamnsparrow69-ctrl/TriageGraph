package in.ac.bitspilani.triagegraph.triage.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;
import in.ac.bitspilani.triagegraph.triage.TriageFixtures.FakeLlm;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

class LlmDecoratorsTest {

    private static final LlmPrompt PROMPT = new LlmPrompt("s", "u", 0, 10);

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void cacheAnswersRepeatedPromptsWithoutCallingTheModelAgain() {
        var llm = new FakeLlm();
        llm.json = Map.of("category", "NETWORK");
        var cached = new CachingLlmClient(llm, 10);
        cached.generateJson(PROMPT, Map.of("type", "OBJECT"));
        cached.generateJson(PROMPT, Map.of("type", "OBJECT"));
        assertEquals(1, llm.prompts.size());
        assertEquals(1, cached.hits());
        cached.generateJson(new LlmPrompt("s", "different", 0, 10), Map.of("type", "OBJECT"));
        assertEquals(2, llm.prompts.size());
        assertTrue(cached.status().contains("1 hits / 2 misses"));
    }

    @Test
    void cacheEvictsTheLeastRecentlyUsedEntryAndNeverCachesEmptyAnswers() {
        var llm = new FakeLlm();
        llm.text = null;
        var cached = new CachingLlmClient(llm, 1);
        cached.generateText(PROMPT);
        cached.generateText(PROMPT);
        assertEquals(2, llm.prompts.size()); // empty answers are retried
        llm.text = "a";
        cached.generateText(new LlmPrompt("s", "one", 0, 10));
        cached.generateText(new LlmPrompt("s", "two", 0, 10));
        cached.generateText(new LlmPrompt("s", "one", 0, 10)); // evicted by "two"
        assertEquals(5, llm.prompts.size());
    }

    /** TC19 (resilience): three failures open the circuit; calls are skipped until a trial succeeds. */
    @Test
    void circuitOpensAfterRepeatedFailuresAndClosesAfterASuccessfulTrial() {
        var llm = new FakeLlm();
        var clock = new MutableClock();
        var breaker = new CircuitBreakerLlmClient(llm, 3, Duration.ofSeconds(60), clock);
        llm.failure = new LlmUnavailableException("timeout");
        for (int i = 0; i < 3; i++) {
            assertTrue(breaker.generateText(PROMPT).isEmpty());
        }
        assertEquals(CircuitBreakerLlmClient.State.OPEN, breaker.state());
        breaker.generateText(PROMPT);
        assertEquals(3, llm.prompts.size(), "open circuit must not call the model");

        clock.now = clock.now.plusSeconds(61);
        llm.failure = null;
        llm.text = "ok";
        assertEquals("ok", breaker.generateText(PROMPT).orElseThrow());
        assertEquals(CircuitBreakerLlmClient.State.CLOSED, breaker.state());
    }

    @Test
    void aFailedTrialReopensTheCircuitImmediately() {
        var llm = new FakeLlm();
        var clock = new MutableClock();
        var breaker = new CircuitBreakerLlmClient(llm, 1, Duration.ofSeconds(60), clock);
        llm.failure = new LlmUnavailableException("down");
        breaker.generateText(PROMPT);
        clock.now = clock.now.plusSeconds(61);
        breaker.generateText(PROMPT); // trial fails
        assertEquals(CircuitBreakerLlmClient.State.OPEN, breaker.state());
        assertTrue(breaker.status().contains("circuit OPEN"));
    }

    @Test
    void noOpClientIsOffAndSaysWhy() {
        var noop = new NoOpLlmClient("no GEMINI_API_KEY set");
        assertFalse(noop.enabled());
        assertTrue(noop.generateText(PROMPT).isEmpty());
        assertEquals("LLM off: no GEMINI_API_KEY set", noop.status());
    }
}

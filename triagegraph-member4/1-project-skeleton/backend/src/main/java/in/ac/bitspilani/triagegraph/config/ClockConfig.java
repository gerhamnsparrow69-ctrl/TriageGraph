package in.ac.bitspilani.triagegraph.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single source of "now". Inject {@link Clock} instead of calling {@code Instant.now()},
 * so tests can pin time with {@code Clock.fixed(...)}.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

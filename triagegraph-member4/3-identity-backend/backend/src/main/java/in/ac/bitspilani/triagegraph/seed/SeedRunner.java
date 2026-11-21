package in.ac.bitspilani.triagegraph.seed;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Runs every {@link DataSeeder} at startup in the dev and demo profiles only. */
@Component
@Profile({"dev", "demo"})
class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final List<DataSeeder> seeders;

    SeedRunner(List<DataSeeder> seeders) {
        this.seeders = seeders;
    }

    @Override
    public void run(ApplicationArguments args) {
        seeders.stream().sorted(Comparator.comparingInt(DataSeeder::order)).forEach(s -> {
            log.info("Seeding {}", s.getClass().getSimpleName());
            s.seed();
        });
    }
}

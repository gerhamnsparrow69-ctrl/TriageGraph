package in.ac.bitspilani.triagegraph.seed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.assignment.application.ExperienceService;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.triage.application.KnowledgeBaseService;
import in.ac.bitspilani.triagegraph.triage.application.KnowledgeBaseService.ArchivedIncident;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;

/**
 * Member 2's seed (order 30, after the catalog and users): 60 resolved campus incidents from
 * {@code triage/historical-incidents.tsv}. They give the kNN classifier and the resolution advisor a
 * history to learn from, and credit each resolver with a fix so the smart strategy has experience data.
 * Safe to run twice: entries are keyed by their HIST-nnn code.
 */
@Component
@Profile({"dev", "demo"})
class HistoricalIncidentSeeder implements DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(HistoricalIncidentSeeder.class);
    static final String RESOURCE = "/triage/historical-incidents.tsv";

    record Row(String code, int daysAgo, String category, String service, String resolver, String title,
            String description, String resolution, String rootCause) {
    }

    private final KnowledgeBaseService knowledge;
    private final ExperienceService experience;
    private final UserDirectory directory;
    private final ServiceCatalog catalog;
    private final Clock clock;

    HistoricalIncidentSeeder(KnowledgeBaseService knowledge, ExperienceService experience, UserDirectory directory,
            ServiceCatalog catalog, Clock clock) {
        this.knowledge = knowledge;
        this.experience = experience;
        this.directory = directory;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void seed() {
        Map<String, Long> technicianIds = directory.technicians().stream()
                .collect(Collectors.toMap(TechnicianProfile::name, TechnicianProfile::id, (a, b) -> a));
        Instant now = Instant.now(clock);
        int added = 0;
        for (Row r : load(getClass().getResourceAsStream(RESOURCE))) {
            Instant created = now.minus(Duration.ofDays(r.daysAgo())).minus(Duration.ofHours(3));
            Instant resolved = created.plus(Duration.ofHours(2));
            Long resolverId = technicianIds.get(r.resolver());
            if (knowledge.addArchived(new ArchivedIncident(r.code(), r.title(), r.description(), r.category(),
                    r.service(), r.resolution(), r.rootCause(), resolverId, created, resolved))) {
                added++;
            }
            if (resolverId != null) {
                Long serviceId = catalog.byName(r.service()).map(ServiceCatalog.ServiceInfo::id).orElse(null);
                experience.recordFix(resolverId, serviceId, r.service(), r.code(), resolved);
            }
        }
        log.info("Seeded {} historical incidents into the triage knowledge base", added);
    }

    static List<Row> load(InputStream in) {
        if (in == null) {
            throw new IllegalStateException("Seed file not found: " + RESOURCE);
        }
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).toList();
            Function<String[], Row> toRow = f -> new Row(f[0], Integer.parseInt(f[1]), f[2], f[3], f[4], f[5], f[6], f[7], f[8]);
            for (String line : lines.subList(1, lines.size())) { // skip the header
                String[] f = line.split("\t", -1);
                if (f.length != 9) {
                    throw new IllegalStateException("Bad seed line (expected 9 columns): " + line);
                }
                rows.add(toRow.apply(f));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }
}

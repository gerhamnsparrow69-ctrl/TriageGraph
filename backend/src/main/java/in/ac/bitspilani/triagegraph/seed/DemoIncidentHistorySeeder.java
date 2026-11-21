package in.ac.bitspilani.triagegraph.seed;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo profile only: two weeks of finished incidents, so the reports, search page and trend chart have
 * something to show before anyone reports a live incident in the video. Every row is resolved or closed in
 * the past, so nothing here is picked up by the SLA monitor or the assignment queues. Runs once: it does
 * nothing if the incidents table already has rows. Deterministic (fixed Random seed, times from the Clock).
 */
@Component
@Profile("demo")
class DemoIncidentHistorySeeder implements DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoIncidentHistorySeeder.class);

    static final int INCIDENTS = 56;
    static final int DAYS = 14;

    /** Service name, category, typical title, typical root cause. */
    record Template(String service, String category, String title, String rootCause) {
    }

    static final List<Template> TEMPLATES = List.of(
            new Template("Fee Payment App", "APPLICATION", "Fee payment page times out at checkout", "Payment gateway API latency"),
            new Template("Payment Gateway API", "APPLICATION", "Payment confirmation not received", "Expired gateway certificate"),
            new Template("Campus SSO", "AUTHENTICATION", "Cannot sign in through campus SSO", "SSO certificate rotation missed"),
            new Template("LDAP Directory", "AUTHENTICATION", "Password reset not taking effect", "LDAP replication lag"),
            new Template("Core Campus Network", "NETWORK", "Whole building lost wired network", "Core switch port flapping"),
            new Template("DNS/DHCP", "NETWORK", "Laptops not getting an IP address", "DHCP scope exhausted"),
            new Template("Database Cluster", "DATABASE", "ERP reports failing with database errors", "Database connection pool exhausted"),
            new Template("LMS", "SOFTWARE", "LMS quiz submission fails", "LMS plugin update regression"),
            new Template("ERP Portal", "SOFTWARE", "ERP report export very slow", "Missing index on ERP report query"),
            new Template("Email & Collaboration", "SOFTWARE", "Emails stuck in outbox", "Mail relay queue backlog"),
            new Template("Hostel Wi-Fi", "NETWORK", "Hostel Wi-Fi keeps disconnecting", "Access point firmware bug"),
            new Template("Academic Wi-Fi", "NETWORK", "Slow Wi-Fi in lecture hall", "Channel congestion on access points"),
            new Template("Library Portal", "SOFTWARE", "Library e-journal access denied", "Proxy licence list outdated"),
            new Template("Cloud Storage", "STORAGE", "Shared drive sync stuck", "Storage quota reached"),
            new Template("Lab Workstations", "HARDWARE", "Lab PC does not boot", "Failed power supply unit"),
            new Template("Print Services", "HARDWARE", "Printer queue not clearing", "Print spooler crash"));

    /** Priority, impact, urgency, resolution target in hours, response target in minutes. */
    record Band(String priority, String impact, String urgency, int resolutionHours, int responseMinutes) {
    }

    static final List<Band> BANDS = List.of(
            new Band("P1", "EXTENSIVE", "CRITICAL", 4, 15),
            new Band("P2", "SIGNIFICANT", "HIGH", 8, 30),
            new Band("P2", "SIGNIFICANT", "HIGH", 8, 30),
            new Band("P3", "MODERATE", "MEDIUM", 24, 120),
            new Band("P3", "MODERATE", "MEDIUM", 24, 120),
            new Band("P3", "MODERATE", "MEDIUM", 24, 120),
            new Band("P4", "MINOR", "LOW", 72, 480));

    private static final List<String> REPORTERS = List.of("dev.sharma", "rahul.verma", "fatima.khan");
    private static final List<String> SOLVERS = List.of("ravi.kumar", "sneha.reddy", "arjun.mehta", "priya.nair",
            "karthik.rao", "ananya.iyer");
    private static final List<String> SOURCES = List.of("KNN", "KNN", "KNN", "KEYWORD", "KEYWORD", "USER");

    private static final String INSERT = """
            INSERT INTO incidents (title, description, reporter_id, category_code, service_id, impact, urgency,
                affected_users, priority, priority_score, priority_rationale, priority_calculator, status,
                assignee_id, assignment_strategy, escalation_level, reopen_count, classification_source,
                classification_confidence, classification_explanation, sla_policy, sla_started_at,
                sla_response_due_at, sla_resolution_due_at, sla_responded_at, sla_warning_percent,
                resolution_summary, resolution_root_cause, resolved_at, resolved_by_id, closed_at, closed_by_id,
                feedback_rating, feedback_comment, feedback_at, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, 75, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    DemoIncidentHistorySeeder(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    @Transactional
    public void seed() {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM incidents", Long.class);
        if (existing != null && existing > 0) {
            return;
        }
        Map<String, Long> users = new HashMap<>();
        jdbc.query("SELECT id, email FROM users", rs -> {
            users.put(rs.getString("email").replace("@triagegraph.local", ""), rs.getLong("id"));
        });
        Map<String, Long> services = new HashMap<>();
        jdbc.query("SELECT id, name FROM it_services", rs -> {
            services.put(rs.getString("name"), rs.getLong("id"));
        });
        if (!users.keySet().containsAll(REPORTERS) || !users.keySet().containsAll(SOLVERS) || services.isEmpty()) {
            log.warn("Demo incident history skipped: demo users or services are missing");
            return;
        }
        Instant now = clock.instant();
        Random random = new Random(42);
        int written = 0;
        for (int n = 0; n < INCIDENTS; n++) {
            Template t = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
            Long serviceId = services.get(t.service());
            if (serviceId == null) {
                continue;
            }
            Band band = BANDS.get(random.nextInt(BANDS.size()));
            // Spread over the last 14 days, oldest first, never in the last 6 hours.
            Instant created = now.minus(Duration.ofHours(6))
                    .minus(Duration.ofMinutes((long) (DAYS * 24 * 60 - 360) * (INCIDENTS - n) / INCIDENTS))
                    .minus(Duration.ofMinutes(random.nextInt(120)));
            Instant responseDue = created.plus(Duration.ofMinutes(band.responseMinutes()));
            Instant resolutionDue = created.plus(Duration.ofHours(band.resolutionHours()));
            Instant responded = created.plus(Duration.ofMinutes(3 + random.nextInt(band.responseMinutes())));
            // About one in eight misses its resolution target.
            double share = random.nextInt(8) == 0 ? 1.1 + random.nextDouble() * 0.6 : 0.2 + random.nextDouble() * 0.7;
            Instant resolved = created.plus(Duration.ofMinutes((long) (band.resolutionHours() * 60 * share)));
            if (resolved.isAfter(now.minus(Duration.ofHours(1)))) {
                resolved = now.minus(Duration.ofHours(1));
            }
            boolean closedToo = resolved.isBefore(now.minus(Duration.ofHours(24))) || random.nextBoolean();
            Instant closed = closedToo ? resolved.plus(Duration.ofMinutes(30 + random.nextInt(600))) : null;
            if (closed != null && closed.isAfter(now)) {
                closed = null;
            }
            long reporter = users.get(REPORTERS.get(random.nextInt(REPORTERS.size())));
            long solver = users.get(SOLVERS.get(random.nextInt(SOLVERS.size())));
            String source = SOURCES.get(random.nextInt(SOURCES.size()));
            Integer rating = closed != null && random.nextInt(3) > 0 ? 3 + random.nextInt(3) : null;
            jdbc.update(INSERT,
                    t.title(), t.title() + ". Reported from the demo history.", reporter, t.category(), serviceId,
                    band.impact(), band.urgency(), 1 + random.nextInt(band.priority().equals("P1") ? 400 : 40),
                    band.priority(), score(band.priority()), "Seeded demo history", "WEIGHTED_SCORE",
                    closed != null ? "CLOSED" : "RESOLVED", solver, "SMART", source,
                    "USER".equals(source) ? 1.0 : 0.62 + random.nextInt(35) / 100.0, "Demo history", "DEFAULT_" + band.priority(),
                    ts(created), ts(responseDue), ts(resolutionDue), ts(responded),
                    "Fixed: " + t.rootCause().toLowerCase(Locale.ROOT) + ".", t.rootCause(), ts(resolved), solver,
                    ts(closed), closed != null ? reporter : null, rating, rating == null ? null : "Thanks, sorted.",
                    rating == null ? null : ts(closed), ts(created), ts(closed != null ? closed : resolved));
            written++;
        }
        log.info("Seeded {} finished demo incidents over the last {} days", written, DAYS);
    }

    private static double score(String priority) {
        return switch (priority) {
            case "P1" -> 52.0;
            case "P2" -> 27.5;
            case "P3" -> 12.0;
            default -> 4.0;
        };
    }

    private static Timestamp ts(Instant t) {
        return t == null ? null : Timestamp.from(t);
    }
}

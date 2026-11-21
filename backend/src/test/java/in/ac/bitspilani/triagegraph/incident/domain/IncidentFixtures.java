package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Incidents in any status, reached through the real transitions (never by setting the status). */
public final class IncidentFixtures {

    public static final long REPORTER_ID = 10, TECH_ID = 20, OTHER_TECH_ID = 21, LEAD_ID = 30, ADMIN_ID = 40;
    public static final Actor REPORTER = Actor.of(REPORTER_ID, Role.EMPLOYEE);
    public static final Actor OTHER_EMPLOYEE = Actor.of(11, Role.EMPLOYEE);
    public static final Actor TECH = Actor.of(TECH_ID, Role.TECHNICIAN);
    public static final Actor OTHER_TECH = Actor.of(OTHER_TECH_ID, Role.TECHNICIAN);
    public static final Actor LEAD = Actor.of(LEAD_ID, Role.TEAM_LEAD);
    public static final Actor ADMIN = Actor.of(ADMIN_ID, Role.ADMIN);
    public static final Instant T0 = Instant.parse("2026-11-20T09:00:00Z");
    public static final SlaTargets P1_SLA =
            new SlaTargets(Duration.ofMinutes(15), Duration.ofHours(4), 75, "Test P1");

    private static long nextId = 1;
    private static long noteIds = 1;

    private IncidentFixtures() {
    }

    public static Incident newIncident() {
        Incident i = Incident.builder()
                .title("Fee payment app unavailable")
                .description("Students cannot complete transactions")
                .reporterId(REPORTER_ID)
                .impact(Impact.EXTENSIVE)
                .urgency(Urgency.CRITICAL)
                .affectedUsers(300)
                .createdAt(T0)
                .build();
        giveId(i, nextId++);
        return i;
    }

    public static void giveId(Incident i, long id) {
        ReflectionTestUtils.setField(i, "id", id);
    }

    /** An incident that has just reached {@code status}, with its events already pulled. */
    public static Incident inStatus(IncidentStatus status) {
        Incident i = newIncident();
        if (status == IncidentStatus.MERGED) {
            i.mergeInto(newIncident(), Actor.system(), T0);
            pull(i);
            return i;
        }
        IncidentStatus[] path = {IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS};
        for (IncidentStatus step : path) {
            if (i.getStatus() == status) {
                break;
            }
            advance(i, step);
        }
        switch (status) {
            case PENDING_INFO -> i.requestInfo("Which browser?", TECH, T0.plusSeconds(500));
            case RESOLVED -> resolve(i);
            case VERIFIED -> {
                resolve(i);
                i.verify(REPORTER, T0.plusSeconds(700));
            }
            case CLOSED -> {
                resolve(i);
                i.verify(REPORTER, T0.plusSeconds(700));
                i.close(TECH, T0.plusSeconds(800));
            }
            default -> {
            }
        }
        if (i.getStatus() != status) {
            throw new IllegalStateException("fixture could not reach " + status);
        }
        pull(i);
        return i;
    }

    /** What saving does before events are pulled: give new notes their generated ids. */
    public static void simulateSave(Incident i) {
        for (InvestigationNote n : i.getNotes()) {
            if (n.getId() == null) {
                ReflectionTestUtils.setField(n, "id", noteIds++);
            }
        }
    }

    public static List<IncidentEvent> pull(Incident i) {
        simulateSave(i);
        return i.pullEvents();
    }

    private static void advance(Incident i, IncidentStatus from) {
        switch (from) {
            case NEW -> {
                i.classify("APPLICATION", 1, new ClassificationInfo("USER", 1.0, "test"), Actor.system(), T0);
                i.applyPriority(new PriorityAssessment(Priority.P1, 83.2, "WEIGHTED", "test"), Actor.system(), T0);
                i.startSla(P1_SLA, T0);
            }
            case CLASSIFIED -> i.assignTo(TECH_ID, "TEST", Actor.system(), T0.plusSeconds(10));
            case ASSIGNED -> i.acknowledge(TECH, T0.plusSeconds(100));
            case ACKNOWLEDGED -> i.startWork(TECH, T0.plusSeconds(200));
            default -> {
            }
        }
    }

    private static void resolve(Incident i) {
        i.resolve("Restarted the payment gateway", "TLS certificate expired", TECH, T0.plusSeconds(600));
    }
}

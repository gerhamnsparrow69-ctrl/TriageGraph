package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Creates new incidents (Factory). The only place that calls {@link Incident#builder()} for live
 * reports, so every incident starts the same way: in NEW, stamped with the injected clock, with an
 * IncidentReported event pending.
 */
@Component
public class IncidentFactory {

    private final Clock clock;

    public IncidentFactory(Clock clock) {
        this.clock = clock;
    }

    public Incident create(ReportIncidentCommand cmd, long reporterId) {
        return create(cmd, reporterId, Instant.now(clock));
    }

    /** For seeders that backdate incidents. */
    public Incident create(ReportIncidentCommand cmd, long reporterId, Instant createdAt) {
        return Incident.builder()
                .title(cmd.title())
                .description(cmd.description())
                .reporterId(reporterId)
                .impact(cmd.impact())
                .urgency(cmd.urgency())
                .affectedUsers(cmd.affectedUsers())
                .createdAt(createdAt)
                .build();
    }
}

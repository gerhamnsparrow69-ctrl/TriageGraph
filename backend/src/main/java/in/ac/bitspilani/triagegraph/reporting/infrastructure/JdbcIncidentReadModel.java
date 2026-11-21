package in.ac.bitspilani.triagegraph.reporting.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.reporting.application.IncidentReadModel;
import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact;
import in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/**
 * Read side over the frozen schema (docs/db/schema.sql), in plain SQL that runs on PostgreSQL and H2. It only
 * SELECTs: reporting never writes another module's tables. Joins pick up the service, reporter and assignee
 * names so the search page needs one request.
 */
@Component
@Transactional(readOnly = true)
public class JdbcIncidentReadModel implements IncidentReadModel {

    /** Same numbering as the incident module: INC- followed by id + 1000. */
    static final long CODE_OFFSET = 1000;

    private static final String INCIDENTS = """
            SELECT i.id, i.title, i.description, i.status, i.priority, i.category_code, i.service_id,
                   s.name AS service_name, i.reporter_id, r.name AS reporter_name, i.assignee_id,
                   a.name AS assignee_name, i.classification_source, i.created_at, i.sla_responded_at,
                   i.resolved_at, i.sla_response_due_at, i.sla_resolution_due_at, i.sla_started_at,
                   i.sla_paused_at, i.sla_warning_percent, i.major_incident_id, i.duplicate_of_id,
                   i.resolution_root_cause
              FROM incidents i
              LEFT JOIN it_services s ON s.id = i.service_id
              LEFT JOIN users r ON r.id = i.reporter_id
              LEFT JOIN users a ON a.id = i.assignee_id
            """;

    private final JdbcTemplate jdbc;

    public JdbcIncidentReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<IncidentFact> incidents() {
        Map<Long, Set<Long>> followers = new HashMap<>();
        jdbc.query("SELECT incident_id, user_id FROM incident_followers", rs -> {
            followers.computeIfAbsent(rs.getLong(1), k -> new HashSet<>()).add(rs.getLong(2));
        });
        return jdbc.query(INCIDENTS, (rs, n) -> incident(rs, followers));
    }

    @Override
    public List<TechnicianFact> technicians() {
        return jdbc.query("""
                SELECT id, name, role, max_load, available, team_lead_id FROM users
                 WHERE role IN ('TECHNICIAN', 'TEAM_LEAD') AND active = TRUE ORDER BY name
                """, (rs, n) -> new TechnicianFact(rs.getLong("id"), rs.getString("name"), rs.getString("role"),
                rs.getInt("max_load"), rs.getBoolean("available"), longOrNull(rs, "team_lead_id")));
    }

    @Override
    public List<ServiceFact> services() {
        return jdbc.query("SELECT id, name, criticality FROM it_services ORDER BY name",
                (rs, n) -> new ServiceFact(rs.getLong("id"), rs.getString("name"), rs.getString("criticality")));
    }

    @Override
    public List<String> categories() {
        return jdbc.queryForList("SELECT code FROM categories WHERE active = TRUE ORDER BY code", String.class);
    }

    @Override
    public Set<Long> teamMembers(long teamLeadId) {
        return new HashSet<>(jdbc.queryForList("SELECT id FROM users WHERE team_lead_id = ?", Long.class, teamLeadId));
    }

    private static IncidentFact incident(ResultSet rs, Map<Long, Set<Long>> followers) throws SQLException {
        long id = rs.getLong("id");
        String priority = rs.getString("priority");
        return new IncidentFact(id, "INC-" + (id + CODE_OFFSET), rs.getString("title"), rs.getString("description"),
                IncidentStatus.valueOf(rs.getString("status")), priority == null ? null : Priority.valueOf(priority),
                rs.getString("category_code"), longOrNull(rs, "service_id"), rs.getString("service_name"),
                rs.getLong("reporter_id"), rs.getString("reporter_name"), longOrNull(rs, "assignee_id"),
                rs.getString("assignee_name"), rs.getString("classification_source"), instant(rs, "created_at"),
                instant(rs, "sla_responded_at"), instant(rs, "resolved_at"), instant(rs, "sla_response_due_at"),
                instant(rs, "sla_resolution_due_at"), instant(rs, "sla_started_at"), instant(rs, "sla_paused_at"),
                intOrNull(rs, "sla_warning_percent"), longOrNull(rs, "major_incident_id"),
                longOrNull(rs, "duplicate_of_id"), rs.getString("resolution_root_cause"),
                followers.getOrDefault(id, Set.of()));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    private static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }
}

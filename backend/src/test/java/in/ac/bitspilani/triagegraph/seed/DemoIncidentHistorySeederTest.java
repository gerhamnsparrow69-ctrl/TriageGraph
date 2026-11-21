package in.ac.bitspilani.triagegraph.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

class DemoIncidentHistorySeederTest {

    private static final Instant NOW = Instant.parse("2026-11-22T06:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DemoIncidentHistorySeeder seeder =
            new DemoIncidentHistorySeeder(jdbc, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void doesNothingWhenIncidentsAlreadyExist() {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM incidents", Long.class)).thenReturn(3L);
        seeder.seed();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void skipsWhenDemoUsersAreMissing() {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM incidents", Long.class)).thenReturn(0L);
        rows("SELECT id, email", Map.of("dev.sharma@triagegraph.local", 9L));
        rows("SELECT id, name", Map.of("LMS", 9L));
        seeder.seed();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void writesFinishedIncidentsInThePastOnly() {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM incidents", Long.class)).thenReturn(0L);
        Map<String, Long> users = new java.util.HashMap<>();
        long id = 1;
        for (String u : List.of("dev.sharma", "rahul.verma", "fatima.khan", "ravi.kumar", "sneha.reddy", "arjun.mehta",
                "priya.nair", "karthik.rao", "ananya.iyer")) {
            users.put(u + "@triagegraph.local", id++);
        }
        Map<String, Long> services = new java.util.HashMap<>();
        for (DemoIncidentHistorySeeder.Template t : DemoIncidentHistorySeeder.TEMPLATES) {
            services.put(t.service(), id++);
        }
        rows("SELECT id, email", users);
        rows("SELECT id, name", services);

        seeder.seed();

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(DemoIncidentHistorySeeder.INCIDENTS)).update(startsWith("INSERT INTO incidents"), args.capture());
        List<Object[]> all = new ArrayList<>(args.getAllValues());
        assertThat(all).allSatisfy(a -> {
            assertThat(a[12]).isIn("RESOLVED", "CLOSED"); // status
            java.sql.Timestamp created = (java.sql.Timestamp) a[32];
            java.sql.Timestamp resolved = (java.sql.Timestamp) a[25];
            assertThat(created.toInstant()).isBefore(resolved.toInstant());
            assertThat(resolved.toInstant()).isBefore(NOW);
            assertThat(created.toInstant()).isAfter(NOW.minus(java.time.Duration.ofDays(DemoIncidentHistorySeeder.DAYS + 1)));
        });
        assertThat(all.stream().map(a -> a[12]).distinct()).contains("CLOSED");
    }

    private void rows(String sqlStart, Map<String, Long> rows) {
        doAnswer(inv -> {
            RowCallbackHandler handler = inv.getArgument(1);
            String column = sqlStart.contains("email") ? "email" : "name";
            for (Map.Entry<String, Long> e : rows.entrySet()) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString(column)).thenReturn(e.getKey());
                when(rs.getLong("id")).thenReturn(e.getValue());
                handler.processRow(rs);
            }
            return null;
        }).when(jdbc).query(startsWith(sqlStart), any(RowCallbackHandler.class));
    }
}

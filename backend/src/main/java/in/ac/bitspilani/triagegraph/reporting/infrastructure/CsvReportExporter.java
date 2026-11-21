package in.ac.bitspilani.triagegraph.reporting.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.reporting.application.IncidentRow;
import in.ac.bitspilani.triagegraph.reporting.application.ReportExporter;

/**
 * RFC 4180 CSV. Cells that start with = + - @ are prefixed with ' so a spreadsheet never runs them as a
 * formula (CSV injection: incident titles are typed by users).
 */
@Component
public class CsvReportExporter implements ReportExporter {

    static final String HEADER = "Code,Title,Status,Priority,Category,Service,Reporter,Assignee,SLA status,"
            + "Resolution due,Major incident,Reported at,Resolved at";

    @Override
    public String format() {
        return "csv";
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public byte[] export(List<IncidentRow> rows) {
        String body = rows.stream().map(CsvReportExporter::line).collect(Collectors.joining("\r\n"));
        String csv = rows.isEmpty() ? HEADER + "\r\n" : HEADER + "\r\n" + body + "\r\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private static String line(IncidentRow r) {
        return Stream.of(r.code(), r.title(), String.valueOf(r.status()), text(r.priority()), r.categoryCode(),
                        r.serviceName(), r.reporterName(), r.assigneeName(), String.valueOf(r.slaStatus()),
                        time(r.resolutionDueAt()), r.major() ? "yes" : "no", time(r.createdAt()), time(r.resolvedAt()))
                .map(CsvReportExporter::cell)
                .collect(Collectors.joining(","));
    }

    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static String text(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String time(Instant t) {
        return t == null ? "" : t.toString();
    }
}

package in.ac.bitspilani.triagegraph.reporting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.reporting.application.IncidentRow;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

class CsvReportExporterTest {

    private final CsvReportExporter exporter = new CsvReportExporter();

    @Test
    void headerAndOneLinePerIncident() {
        IncidentRow row = new IncidentRow(4, "INC-1004", "Wi-Fi down, \"again\"", IncidentStatus.ASSIGNED, Priority.P2,
                "NETWORK", 12L, "Hostel Wi-Fi", 9, "Rahul Verma", 3L, "Ravi Kumar", SlaStatus.AT_RISK,
                Instant.parse("2026-11-21T12:00:00Z"), false, Instant.parse("2026-11-21T04:00:00Z"), null);
        String csv = new String(exporter.export(List.of(row)), StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo(CsvReportExporter.HEADER);
        assertThat(lines[1]).isEqualTo("INC-1004,\"Wi-Fi down, \"\"again\"\"\",ASSIGNED,P2,NETWORK,Hostel Wi-Fi,"
                + "Rahul Verma,Ravi Kumar,AT_RISK,2026-11-21T12:00:00Z,no,2026-11-21T04:00:00Z,");
    }

    @Test
    void emptyResultIsJustTheHeader() {
        assertThat(new String(exporter.export(List.of()), StandardCharsets.UTF_8)).isEqualTo(CsvReportExporter.HEADER + "\r\n");
        assertThat(exporter.format()).isEqualTo("csv");
        assertThat(exporter.contentType()).isEqualTo("text/csv");
    }

    @Test
    void formulaLookingCellsAreDefused() {
        assertThat(CsvReportExporter.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(CsvReportExporter.cell("+91 phone")).isEqualTo("'+91 phone");
        assertThat(CsvReportExporter.cell(null)).isEmpty();
        assertThat(CsvReportExporter.cell("plain")).isEqualTo("plain");
    }
}

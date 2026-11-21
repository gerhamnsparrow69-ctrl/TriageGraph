package in.ac.bitspilani.triagegraph.reporting.application;

import java.util.List;

/** Turns a search result into a downloadable file. CSV today; a PDF or Excel exporter would be one more class. */
public interface ReportExporter {

    String format();

    String contentType();

    byte[] export(List<IncidentRow> rows);
}

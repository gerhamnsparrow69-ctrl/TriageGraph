package in.ac.bitspilani.triagegraph.sla.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.sla.infrastructure.SlaClockRepository;

@Service
@Transactional(readOnly = true)
public class SlaReportingService {

    private final SlaClockRepository clocks;

    public SlaReportingService(SlaClockRepository clocks) {
        this.clocks = clocks;
    }

    public SlaBreachReport breachCounts() {
        SlaClockRepository.SlaBreachCounts counts = clocks.countBreachCounts();
        return new SlaBreachReport(
                counts.getResponseBreachCount(),
                counts.getResolutionBreachCount(),
                counts.getIncidentsWithBreaches());
    }
}

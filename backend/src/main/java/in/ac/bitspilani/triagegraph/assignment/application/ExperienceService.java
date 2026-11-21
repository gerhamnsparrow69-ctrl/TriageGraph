package in.ac.bitspilani.triagegraph.assignment.application;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFix;
import in.ac.bitspilani.triagegraph.assignment.domain.TechnicianFixRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;

/**
 * Keeps the ledger of who fixed what, and adds those counts to the technician profiles that come from
 * Member 4's user directory (whose {@code fixesByService} is empty because identity has no incident history).
 */
@Service
public class ExperienceService {

    private static final Logger log = LoggerFactory.getLogger(ExperienceService.class);

    private final TechnicianFixRepository fixes;

    public ExperienceService(TechnicianFixRepository fixes) {
        this.fixes = fixes;
    }

    /** Records one fix. Returns false when this incident was already recorded (idempotent). */
    @Transactional
    public boolean recordFix(long technicianId, Long serviceId, String serviceName, String incidentCode, Instant fixedAt) {
        if (incidentCode == null || incidentCode.isBlank() || fixes.existsByIncidentCode(incidentCode)) {
            return false;
        }
        fixes.save(new TechnicianFix(technicianId, serviceId, serviceName, incidentCode, fixedAt));
        log.debug("Recorded fix {} for technician {} on service {}", incidentCode, technicianId, serviceId);
        return true;
    }

    /** technicianId → (serviceId → number of fixes). Fixes without a known service id are skipped. */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, Integer>> fixesByTechnician() {
        Map<Long, Map<Long, Integer>> result = new HashMap<>();
        for (TechnicianFix f : fixes.findAll()) {
            if (f.getServiceId() != null) {
                result.computeIfAbsent(f.getTechnicianId(), k -> new HashMap<>())
                        .merge(f.getServiceId(), 1, Integer::sum);
            }
        }
        return result;
    }

    /** Copies each profile with its fix counts merged in (the directory's own counts are kept, ledger added). */
    public List<TechnicianProfile> enrich(List<TechnicianProfile> profiles) {
        Map<Long, Map<Long, Integer>> ledger = fixesByTechnician();
        return profiles.stream().map(p -> withFixes(p, ledger.getOrDefault(p.id(), Map.of()))).toList();
    }

    static TechnicianProfile withFixes(TechnicianProfile p, Map<Long, Integer> extra) {
        if (extra.isEmpty()) {
            return p;
        }
        Map<Long, Integer> merged = new HashMap<>(p.fixesByService() == null ? Map.of() : p.fixesByService());
        extra.forEach((service, n) -> merged.merge(service, n, Integer::sum));
        return new TechnicianProfile(p.id(), p.name(), p.skills(), p.activeLoad(), p.maxLoad(),
                p.experienceYears(), Map.copyOf(merged), p.available());
    }
}

package in.ac.bitspilani.triagegraph.assignment.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One resolved incident in a technician's history. The ledger feeds the "past fixes on this service"
 * part of the smart score. One row per incident code, so recording the same fix twice is harmless.
 */
@Entity
@Table(name = "technician_fix", indexes = @Index(name = "ix_technician_fix_tech", columnList = "technician_id"))
public class TechnicianFix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "technician_id", nullable = false)
    private long technicianId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "incident_code", nullable = false, unique = true, length = 40)
    private String incidentCode;

    @Column(name = "fixed_at", nullable = false)
    private Instant fixedAt;

    protected TechnicianFix() {
    }

    public TechnicianFix(long technicianId, Long serviceId, String serviceName, String incidentCode, Instant fixedAt) {
        this.technicianId = technicianId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.incidentCode = Objects.requireNonNull(incidentCode);
        this.fixedAt = Objects.requireNonNull(fixedAt);
    }

    public Long getId() {
        return id;
    }

    public long getTechnicianId() {
        return technicianId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public Instant getFixedAt() {
        return fixedAt;
    }
}

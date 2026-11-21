package in.ac.bitspilani.triagegraph.shared.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;

class ContractsTest {

    @Test
    void weightsRunFromOneToFour() {
        assertThat(Impact.MINOR.weight()).isEqualTo(1);
        assertThat(Impact.EXTENSIVE.weight()).isEqualTo(4);
        assertThat(Urgency.CRITICAL.weight()).isEqualTo(4);
        assertThat(Criticality.MISSION_CRITICAL.weight()).isEqualTo(4);
    }

    @Test
    void lifecycleHasTenStates() {
        assertThat(IncidentStatus.values()).hasSize(10);
    }

    @Test
    void eventsAreSealedSoListenersCanSwitchExhaustively() {
        DomainEvent e = new IncidentStatusChanged(1L, "INC-0001", 7L, Instant.EPOCH,
                IncidentStatus.NEW, IncidentStatus.CLASSIFIED, "auto");
        String kind = switch (e) {
            case IncidentEvent ie -> "incident " + ie.incidentCode();
            case in.ac.bitspilani.triagegraph.shared.contracts.events.MajorIncidentDeclared m -> "major";
            case in.ac.bitspilani.triagegraph.shared.contracts.events.CatalogChanged c -> "catalog";
        };
        assertThat(kind).isEqualTo("incident INC-0001");
        assertThat(DomainEvent.class.isSealed()).isTrue();
    }
}

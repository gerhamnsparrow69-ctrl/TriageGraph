package in.ac.bitspilani.triagegraph.topology.rca.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EvidenceAggregatorTest {

    @Test
    void retainsHighestBonusPerTypeCapsTotalAndOrdersEvidenceDeterministically() {
        EvidenceAggregator aggregator = new EvidenceAggregator(0.15);
        AggregatedEvidence result = aggregator.aggregate(List.of(
                new Evidence(EvidenceType.OPEN_INCIDENT, 0.08, "Open incident on this service"),
                new Evidence(EvidenceType.RECENT_CHANGE, 0.07, "Change logged 20 min ago: older"),
                new Evidence(EvidenceType.RECENT_CHANGE, 0.12, "Change logged 5 min ago: newer"),
                new Evidence(EvidenceType.RECENT_CHANGE, 0.12, "Change logged 5 min ago: alternate")));

        assertThat(result.bonus()).isEqualTo(0.15);
        assertThat(result.evidence()).containsExactly(
                new Evidence(EvidenceType.RECENT_CHANGE, 0.12, "Change logged 5 min ago: alternate"),
                new Evidence(EvidenceType.OPEN_INCIDENT, 0.08, "Open incident on this service"));
    }

    @Test
    void duplicateEvidenceOfOneTypeCannotEarnMultipleBonuses() {
        AggregatedEvidence result = new EvidenceAggregator(0.15).aggregate(List.of(
                new Evidence(EvidenceType.RECENT_CHANGE, 0.10, "Change logged 5 min ago: first"),
                new Evidence(EvidenceType.RECENT_CHANGE, 0.10, "Change logged 1 min ago: second")));

        assertThat(result.bonus()).isEqualTo(0.10);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().get(0).text()).isEqualTo("Change logged 1 min ago: second");
    }
}

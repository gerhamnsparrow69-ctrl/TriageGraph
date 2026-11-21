package in.ac.bitspilani.triagegraph.topology.rca.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RcaScoringTest {

    private SeededTopologyFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SeededTopologyFixture();
    }

    @Test
    void primaryScenarioRanksSsoLdapCorePowerAndDatabaseWithEvidence() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");
        RcaAssessment result = fixture.assess(affected,
                Set.of(fixture.id("Campus SSO")),
                affected);

        assertScores(result,
                "Campus SSO", 0.7833333333333333,
                "LDAP Directory", 0.675,
                "Core Campus Network", 0.646875,
                "Data-Centre Power", 0.6441176470588236,
                "Database Cluster", 0.525);
        assertThat(result.candidates()).extracting(ScoredCandidate::serviceName)
                .startsWith("Campus SSO", "LDAP Directory", "Core Campus Network",
                        "Data-Centre Power", "Database Cluster");
        assertThat(candidate(result, "LMS").score()).isCloseTo(0.5,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "ERP Portal").score()).isCloseTo(0.5,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "Email & Collaboration").score()).isCloseTo(0.5,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "Campus SSO").specificity()).isEqualTo(1.0 / 3.0);
        assertThat(fixture.topology().dependentsOf(fixture.id("Campus SSO"))).hasSize(8);
    }

    @Test
    void primaryScenarioWithoutOpenIncidentsKeepsSsoBonusAndScoresAffectedServicesAtPoint45() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");
        RcaAssessment result = fixture.assess(affected, Set.of(fixture.id("Campus SSO")), Set.of());

        assertThat(candidate(result, "Campus SSO").score()).isCloseTo(0.7833333333333333,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "LMS").score()).isCloseTo(0.45,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "ERP Portal").score()).isCloseTo(0.45,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "Email & Collaboration").score()).isCloseTo(0.45,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void primaryScenarioWithoutSsoChangeIsAmbiguous() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");
        RcaAssessment result = fixture.assess(affected, Set.of(), affected);

        assertThat(candidate(result, "Campus SSO").score()).isCloseTo(0.6833333333333333,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(result, "LDAP Directory").score()).isCloseTo(0.675,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void twoAffectedServicesHaveExpectedDatabaseSsoAndLdapScoresAndChangeCanReorderSso() {
        Set<Long> affected = SeededTopologyFixture.services(fixture, "LMS", "ERP Portal");
        RcaAssessment withoutChange = fixture.assess(affected);
        assertThat(candidate(withoutChange, "Database Cluster").score()).isEqualTo(0.725);
        assertThat(candidate(withoutChange, "Campus SSO").score()).isCloseTo(0.6555555555555556,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(candidate(withoutChange, "LDAP Directory").score()).isEqualTo(0.65);
        assertThat(withoutChange.candidates().get(0).serviceName()).isEqualTo("Database Cluster");

        RcaAssessment withChange = fixture.assess(affected, Set.of(fixture.id("Campus SSO")), Set.of());
        assertThat(candidate(withChange, "Campus SSO").score()).isCloseTo(0.7555555555555555,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(withChange.candidates().get(0).serviceName()).isEqualTo("Campus SSO");
    }

    @Test
    void openIncidentOnUnaffectedLdapServiceAddsEvidenceAndRanksItFirst() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");
        RcaAssessment result = fixture.assess(affected, Set.of(), Set.of(fixture.id("LDAP Directory")));

        assertThat(candidate(result, "LDAP Directory").score()).isEqualTo(0.725);
        assertThat(candidate(result, "Campus SSO").score()).isCloseTo(0.6833333333333333,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(result.candidates().get(0).serviceName()).isEqualTo("LDAP Directory");
    }

    @Test
    void ssoWithBothEvidenceTypesGetsCombinedBonus() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration", "Campus SSO");
        RcaAssessment result = fixture.assess(affected,
                Set.of(fixture.id("Campus SSO")),
                Set.of(fixture.id("Campus SSO")));

        assertThat(candidate(result, "Campus SSO").score()).isCloseTo(0.8611111111111112,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void oneAffectedLmsWithBothEvidenceBonusesIsCappedAtOne() {
        Set<Long> affected = SeededTopologyFixture.services(fixture, "LMS");
        RcaAssessment result = fixture.assess(affected,
                Set.of(fixture.id("LMS")),
                Set.of(fixture.id("LMS")));

        assertThat(candidate(result, "LMS").score()).isEqualTo(1.0);
    }

    @Test
    void noEvidenceAndCloudCandidateScoresFollowImpactSetSpecificity() {
        RcaAssessment lms = fixture.assess(SeededTopologyFixture.services(fixture, "LMS"));
        assertThat(candidate(lms, "LMS").score()).isEqualTo(0.85);
        assertThat(candidate(lms, "Cloud Storage").score()).isEqualTo(0.725);
        assertThat(candidate(lms, "Database Cluster").score()).isEqualTo(0.6625);

        RcaAssessment primary = fixture.assess(SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration"));
        assertThat(candidate(primary, "Cloud Storage").score()).isCloseTo(0.325,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void isolatedServicesHaveUnitSpecificityWithoutDivisionByZero() {
        ImpactTopology isolated = new ImpactTopology() {
            @Override
            public boolean contains(long serviceId) {
                return serviceId == 101 || serviceId == 102;
            }

            @Override
            public Set<Long> serviceIds() {
                return Set.of(101L, 102L);
            }

            @Override
            public Set<Long> dependenciesOf(long serviceId) {
                return Set.of();
            }

            @Override
            public Set<Long> dependentsOf(long serviceId) {
                return Set.of();
            }

            @Override
            public long version() {
                return 1;
            }
        };

        List<Long> candidates = List.of(101L, 102L);
        RcaAssessment result = new RcaScorer(RcaConfig.defaults()).assess(
                candidates, Set.of(101L, 102L), isolated, java.util.Map.of(), java.util.Map.of());

        assertThat(result.candidates()).extracting(ScoredCandidate::specificity)
                .containsExactly(1.0, 1.0);
        assertThat(result.candidates()).extracting(ScoredCandidate::coverage)
                .containsExactly(0.5, 0.5);
    }

    @Test
    void customGraphRequiresOpenIncidentForTheExpectedSRootScore() {
        ImpactTopology topology = customTopology();
        Set<Long> affected = Set.of(1L);
        List<Long> candidates = new CandidateGenerator().generate(affected, topology);
        RcaScorer scorer = new RcaScorer(RcaConfig.defaults());
        java.util.Map<Long, String> names = java.util.Map.of(1L, "S", 2L, "R1", 3L, "R2");

        RcaAssessment withoutOpenIncident = scorer.assess(
                candidates, affected, topology, names, java.util.Map.of());
        RcaAssessment withOpenIncident = scorer.assess(
                candidates,
                affected,
                topology,
                names,
                java.util.Map.of(1L, new AggregatedEvidence(
                        List.of(new Evidence(EvidenceType.OPEN_INCIDENT, 0.05, "Open incident on this service")),
                        0.05)));

        assertThat(candidate(withoutOpenIncident, "S").score()).isEqualTo(0.85);
        assertThat(candidate(withOpenIncident, "S").score()).isEqualTo(0.9);
        assertThat(candidate(withOpenIncident, "R1").score()).isEqualTo(0.725);
        assertThat(candidate(withOpenIncident, "R2").score()).isEqualTo(0.725);
    }

    @Test
    void candidateGenerationIsDeterministicAndResultsObeyScoreInvariants() {
        assertThat(fixture.serviceCount()).isEqualTo(17);
        assertThat(fixture.edgeCount()).isEqualTo(29);
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");
        var first = fixture.assess(affected);
        var shuffled = fixture.assess(new java.util.LinkedHashSet<>(
                List.of(fixture.id("Email & Collaboration"), fixture.id("LMS"), fixture.id("ERP Portal"))));

        assertThat(first).isEqualTo(shuffled);
        assertThat(first.candidates()).hasSizeLessThanOrEqualTo(17)
                .allSatisfy(candidate -> {
                    assertThat(candidate.coverage()).isBetween(0.0, 1.0).isNotEqualTo(0.0);
                    assertThat(candidate.specificity()).isBetween(0.0, 1.0).isNotEqualTo(0.0);
                    assertThat(candidate.score()).isLessThanOrEqualTo(1.0);
                });
        assertThat(first.candidates()).isSortedAccordingTo(RcaScorer.rankingOrder());
    }

    @Test
    void candidateSetContainsAffectedServicesAndOnlyTheirTransitiveDependencies() {
        Set<Long> affected = SeededTopologyFixture.services(
                fixture, "LMS", "ERP Portal", "Email & Collaboration");

        assertThat(fixture.candidates(affected)).containsExactlyInAnyOrder(
                fixture.id("LMS"),
                fixture.id("ERP Portal"),
                fixture.id("Email & Collaboration"),
                fixture.id("Campus SSO"),
                fixture.id("LDAP Directory"),
                fixture.id("Data-Centre Power"),
                fixture.id("Database Cluster"),
                fixture.id("Cloud Storage"),
                fixture.id("DNS/DHCP"),
                fixture.id("Core Campus Network"));
    }

    @Test
    void comparatorUsesSpecificityThenServiceIdWhenRoundedScoresTie() {
        ScoredCandidate equalSpecificityHigherId = scored(8, 0.6, 0.3);
        ScoredCandidate equalSpecificityLowerId = scored(4, 0.6, 0.3);
        ScoredCandidate higherSpecificity = scored(3, 0.6, 0.7);
        List<ScoredCandidate> candidates = new ArrayList<>(
                List.of(equalSpecificityHigherId, higherSpecificity, equalSpecificityLowerId));

        candidates.sort(RcaScorer.rankingOrder());

        assertThat(candidates).containsExactly(
                higherSpecificity, equalSpecificityLowerId, equalSpecificityHigherId);
    }

    @Test
    void comparatorTreatsSubNanopointScoreNoiseAsTie() {
        ScoredCandidate higherScoreLowerSpecificity = scored(9, 0.500000000001, 0.2);
        ScoredCandidate lowerScoreHigherSpecificity = scored(2, 0.5, 0.9);
        List<ScoredCandidate> candidates = new ArrayList<>(
                List.of(lowerScoreHigherSpecificity, higherScoreLowerSpecificity));

        candidates.sort(RcaScorer.rankingOrder());

        assertThat(candidates).containsExactly(lowerScoreHigherSpecificity, higherScoreLowerSpecificity);
    }

    @Test
    void ambiguityThresholdHandlesCloseWideExactAndSingleCandidateGaps() {
        RcaConfig config = RcaConfig.defaults();

        assertThat(isAmbiguous(0.7000, 0.6917, config)).isTrue();
        assertThat(isAmbiguous(0.7000, 0.5920, config)).isFalse();
        assertThat(isAmbiguous(0.80, 0.75, config)).isTrue();
        assertThat(isAmbiguous(0.80, null, config)).isFalse();
    }

    private static boolean isAmbiguous(double top, Double runnerUp, RcaConfig config) {
        List<ScoredCandidate> candidates = runnerUp == null
                ? List.of(scored(1, top, 1))
                : List.of(scored(1, top, 1), scored(2, runnerUp, 0.5));
        return RcaScorer.isAmbiguous(candidates, config);
    }

    private static ScoredCandidate scored(long id, double score, double specificity) {
        return new ScoredCandidate(id, "Service " + id, score, 1, specificity, List.of());
    }

    private static ImpactTopology customTopology() {
        return new ImpactTopology() {
            @Override
            public boolean contains(long serviceId) {
                return serviceId >= 1 && serviceId <= 3;
            }

            @Override
            public Set<Long> serviceIds() {
                return Set.of(1L, 2L, 3L);
            }

            @Override
            public Set<Long> dependenciesOf(long serviceId) {
                return serviceId == 1 ? Set.of(2L, 3L) : Set.of();
            }

            @Override
            public Set<Long> dependentsOf(long serviceId) {
                return serviceId == 2 || serviceId == 3 ? Set.of(1L) : Set.of();
            }

            @Override
            public long version() {
                return 1;
            }
        };
    }

    private static ScoredCandidate candidate(RcaAssessment assessment, String name) {
        return assessment.candidates().stream()
                .filter(candidate -> candidate.serviceName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void assertScores(RcaAssessment result, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            String name = (String) pairs[i];
            double expected = (double) pairs[i + 1];
            assertThat(candidate(result, name).score())
                    .as(name)
                    .isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-12));
        }
    }
}

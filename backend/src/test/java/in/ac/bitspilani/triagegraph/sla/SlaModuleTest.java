package in.ac.bitspilani.triagegraph.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaWarningRaised;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import in.ac.bitspilani.triagegraph.sla.application.DefaultSlaPolicyService;
import in.ac.bitspilani.triagegraph.sla.application.SlaClockService;
import in.ac.bitspilani.triagegraph.sla.domain.SlaClock;
import in.ac.bitspilani.triagegraph.sla.infrastructure.SlaClockRepository;
import in.ac.bitspilani.triagegraph.sla.application.SlaPriorityChangeListener;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.SlaConfigurationMissingException;

class SlaModuleTest {

    private static final Instant T0 = Instant.parse("2026-11-21T09:00:00Z");

    @Test
    void defaultTargetsCoverP1ThroughP4() {
        CatalogQuery catalog = mock(CatalogQuery.class);
        when(catalog.service(100)).thenReturn(new ServiceRef(
                100, "LMS", "LMS", Criticality.HIGH));
        DefaultSlaPolicyService policies = new DefaultSlaPolicyService(catalog);

        assertThat(policies.targetsFor(100, Priority.P1).response()).isEqualTo(Duration.ofMinutes(5));
        assertThat(policies.targetsFor(100, Priority.P1).resolution()).isEqualTo(Duration.ofMinutes(30));
        assertThat(policies.targetsFor(100, Priority.P2).response()).isEqualTo(Duration.ofMinutes(15));
        assertThat(policies.targetsFor(100, Priority.P2).resolution()).isEqualTo(Duration.ofHours(2));
        assertThat(policies.targetsFor(100, Priority.P3).response()).isEqualTo(Duration.ofHours(1));
        assertThat(policies.targetsFor(100, Priority.P3).resolution()).isEqualTo(Duration.ofHours(8));
        assertThat(policies.targetsFor(100, Priority.P4).response()).isEqualTo(Duration.ofHours(4));
        assertThat(policies.targetsFor(100, Priority.P4).resolution()).isEqualTo(Duration.ofHours(24));
        assertThat(policies.targetsFor(100, Priority.P4).warningPercent()).isEqualTo(75);
    }

    @Test
    void feePaymentAppP1UsesOverride() {
        CatalogQuery catalog = mock(CatalogQuery.class);
        when(catalog.service(1)).thenReturn(new ServiceRef(
                1, "Fee Payment App", "Fee Payment App", Criticality.MISSION_CRITICAL));
        DefaultSlaPolicyService policies = new DefaultSlaPolicyService(catalog);

        SlaTargets targets = policies.targetsFor(1, Priority.P1);
        assertThat(targets.response()).isEqualTo(Duration.ofMinutes(3));
        assertThat(targets.resolution()).isEqualTo(Duration.ofMinutes(20));
        assertThat(targets.warningPercent()).isEqualTo(75);
        assertThat(targets.policyName()).isEqualTo("FEE_PAYMENT_APP_P1");
    }

    @Test
    void missingPriorityIsConfigurationErrorAndUnknownServiceRemainsNotFound() {
        CatalogQuery catalog = mock(CatalogQuery.class);
        when(catalog.service(999)).thenThrow(new ServiceNotFoundException("missing"));
        DefaultSlaPolicyService policies = new DefaultSlaPolicyService(catalog);

        assertThatThrownBy(() -> policies.targetsFor(100, null))
                .isInstanceOf(SlaConfigurationMissingException.class);
        assertThatThrownBy(() -> policies.targetsFor(999, Priority.P1))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void warningAt75PercentAndNoWarningBeforeThreshold() {
        IncidentSnapshot incident = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);
        SlaClock clockState = new SlaClock(incident, T0);
        SlaTargets targets = new SlaTargets(Duration.ofMinutes(10),
                Duration.ofMinutes(40), 75, "TEST");

        assertThat(evaluateWarning(clockState, incident, targets, T0.plusSeconds(449))).isFalse();
        assertThat(evaluateWarning(clockState, incident, targets, T0.plusSeconds(450))).isTrue();
    }

    @Test
    void breachAtExactly100Percent() {
        IncidentSnapshot incident = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);
        SlaClock clockState = new SlaClock(incident, T0);
        SlaTargets targets = new SlaTargets(Duration.ofMinutes(10),
                Duration.ofMinutes(40), 75, "TEST");

        assertThat(clockState.elapsedMillis(SlaKind.RESPONSE, T0.plusSeconds(599))).isLessThan(
                targets.response().toMillis());
        clockState.update(incident, T0.plusSeconds(600));
        assertThat(clockState.elapsedMillis(SlaKind.RESPONSE, T0.plusSeconds(600)))
                .isGreaterThanOrEqualTo(targets.response().toMillis());
    }

    @Test
    void warningAndBreachEventsAreNotDuplicated() {
        CatalogQuery catalog = mock(CatalogQuery.class);
        when(catalog.service(100)).thenReturn(new ServiceRef(
                100, "LMS", "LMS", Criticality.HIGH));
        DefaultSlaPolicyService policies = new DefaultSlaPolicyService(catalog);

        SlaClockRepository repository = mock(SlaClockRepository.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        SlaClock clockState = new SlaClock(snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1), T0);
        when(repository.findByIncidentIdForUpdate(1L)).thenReturn(Optional.of(clockState));
        when(repository.save(any(SlaClock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(SlaClock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clock clock = Clock.fixed(T0.plusSeconds(225), ZoneOffset.UTC);
        SlaClockService service = new SlaClockService(policies, repository, publisher, clock);
        IncidentSnapshot incident = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);

        service.process(incident);
        service.process(incident);

        ArgumentCaptor<List<? extends DomainEvent>> events =
                ArgumentCaptor.forClass(List.class);
        verify(publisher, times(1)).publishAll(events.capture());
        assertThat(events.getAllValues().getFirst()).hasSize(1);
        assertThat(events.getAllValues().getFirst().getFirst())
                .isInstanceOf(SlaWarningRaised.class);

        Clock breachClock = Clock.fixed(T0.plusSeconds(300), ZoneOffset.UTC);
        SlaClockService breachService = new SlaClockService(policies, repository, publisher, breachClock);
        breachService.process(incident);
        breachService.process(incident);
        verify(publisher, times(2)).publishAll(any());
    }

    @Test
    void pendingInfoAndResolvedPauseBothClocksAndResumeAddsOnlyActiveTime() {
        IncidentSnapshot active = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);
        SlaClock clockState = new SlaClock(active, T0);

        clockState.update(snapshot(IncidentStatus.PENDING_INFO, T0, Priority.P1),
                T0.plusSeconds(600));
        assertThat(clockState.elapsedMillis(SlaKind.RESPONSE, T0.plusSeconds(1200))).isEqualTo(600_000);

        clockState.update(snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1),
                T0.plusSeconds(1200));
        assertThat(clockState.elapsedMillis(SlaKind.RESPONSE, T0.plusSeconds(1800))).isEqualTo(1_200_000);

        clockState.update(snapshot(IncidentStatus.RESOLVED, T0, Priority.P1),
                T0.plusSeconds(1800));
        assertThat(clockState.elapsedMillis(SlaKind.RESOLUTION, T0.plusSeconds(3600))).isEqualTo(1_200_000);
    }

    @Test
    void responseAndResolutionClockFlagsAreIndependent() {
        IncidentSnapshot incident = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);
        SlaClock clockState = new SlaClock(incident, T0);

        clockState.update(incident, T0.plusSeconds(1800));
        clockState.markWarningRaised(SlaKind.RESPONSE);

        assertThat(clockState.isWarningRaised(SlaKind.RESPONSE)).isTrue();
        assertThat(clockState.isWarningRaised(SlaKind.RESOLUTION)).isFalse();
        assertThat(clockState.isBreachRaised(SlaKind.RESPONSE)).isFalse();
        assertThat(clockState.isBreachRaised(SlaKind.RESOLUTION)).isFalse();
    }

    @Test
    void priorityChangeResetsWarningAndBreachState() {
        IncidentSnapshot incident = snapshot(IncidentStatus.IN_PROGRESS, T0, Priority.P1);
        SlaClock clockState = new SlaClock(incident, T0);
        clockState.markWarningRaised(SlaKind.RESPONSE);
        clockState.markBreachRaised(SlaKind.RESOLUTION);

        clockState.applyPriorityChange(Priority.P4);

        assertThat(clockState.getPriority()).isEqualTo(Priority.P4);
        assertThat(clockState.isWarningRaised(SlaKind.RESPONSE)).isFalse();
        assertThat(clockState.isBreachRaised(SlaKind.RESOLUTION)).isFalse();
    }

    private boolean evaluateWarning(SlaClock clockState, IncidentSnapshot incident,
                                    SlaTargets targets, Instant now) {
        clockState.update(incident, now);
        long elapsed = clockState.elapsedMillis(SlaKind.RESPONSE, now);
        long warningMillis = targets.response().toMillis() * targets.warningPercent() / 100;
        if (elapsed >= warningMillis) {
            clockState.markWarningRaised(SlaKind.RESPONSE);
            return true;
        }
        return false;
    }

    private IncidentSnapshot snapshot(IncidentStatus status, Instant createdAt, Priority priority) {
        return new IncidentSnapshot(
                1L, "INC-0001", "Test incident", "Test description", status,
                priority, 100L, "APPLICATION", 1L, Set.of(), null, null,
                createdAt, createdAt.plusSeconds(300), createdAt.plusSeconds(1800));
    }
}

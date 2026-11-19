package uk.gov.moj.cpp.listing.messaging.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.persistence.repository.inbound.InboundEventDedupJdbcRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class InboundDedupPurgeTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:30:00Z");

    private final InboundEventDedupJdbcRepository repository = mock(InboundEventDedupJdbcRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    InboundDedupPurgeTest() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void deletesInBatchesUntilABatchIsShortWithTheDefaultThirtyDayRetention() {
        final MessagingProperties properties = new MessagingProperties();
        properties.getInbound().getDedup().getPurge().setBatchSize(2);
        when(repository.tryLockForPurge()).thenReturn(true);
        final Instant cutoff = NOW.minus(Duration.ofDays(30));
        when(repository.deleteReceivedBefore(cutoff, 2)).thenReturn(2, 2, 1);

        new InboundDedupPurge(repository, transactionManager, clock, properties).purge();

        verify(repository, times(3)).deleteReceivedBefore(cutoff, 2);
        verify(transactionManager, times(3)).commit(any());
    }

    @Test
    void stopsWhenAnotherNodeHoldsTheLock() {
        when(repository.tryLockForPurge()).thenReturn(false);

        new InboundDedupPurge(repository, transactionManager, clock, new MessagingProperties()).purge();

        verify(repository, never()).deleteReceivedBefore(any(), anyInt());
    }

    @Test
    void neverLetsAnExceptionEscapeTheScheduledRun() {
        when(repository.tryLockForPurge()).thenReturn(true);
        when(repository.deleteReceivedBefore(any(), anyInt())).thenThrow(new IllegalStateException("db down"));

        new InboundDedupPurge(repository, transactionManager, clock, new MessagingProperties()).purge();

        verify(repository).deleteReceivedBefore(any(), anyInt());
    }

    @Test
    void retentionAndScheduleBindFromIsoDurationAndCron() {
        final MessagingProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "listing.messaging.inbound.dedup.retention", "P7D",
                "listing.messaging.inbound.dedup.purge.cron", "0 0 * * * *",
                "listing.messaging.inbound.dedup.purge.batch-size", "50")))
                .bind("listing.messaging", MessagingProperties.class).get();

        assertThat(properties.getInbound().getDedup().getRetention()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.getInbound().getDedup().getPurge().getCron()).isEqualTo("0 0 * * * *");
        when(repository.tryLockForPurge()).thenReturn(true);

        new InboundDedupPurge(repository, transactionManager, clock, properties).purge();

        verify(repository).deleteReceivedBefore(NOW.minus(Duration.ofDays(7)), 50);
    }

    @Test
    void rejectsANonPositiveRetention() {
        final MessagingProperties properties = new MessagingProperties();
        properties.getInbound().getDedup().setRetention(Duration.ZERO);

        assertThatThrownBy(() -> new InboundDedupPurge(repository, transactionManager, clock, properties))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("retention");
    }
}

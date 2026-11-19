package uk.gov.moj.cpp.listing.command.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AfterCommitTest {

    private final List<Runnable> submitted = new ArrayList<>();
    private final AfterCommit afterCommit = new AfterCommit(submitted::add);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void inATransactionTheStepIsSubmittedOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        final List<String> ran = new ArrayList<>();

        afterCommit.run("step", () -> ran.add("step"));

        assertThat(submitted).isEmpty();
        final List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        assertThat(submitted).hasSize(1);
        submitted.get(0).run();
        assertThat(ran).containsExactly("step");
    }

    @Test
    void aRolledBackTransactionNeverSubmitsTheStep() {
        TransactionSynchronizationManager.initSynchronization();

        afterCommit.run("step", () -> { });

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(submitted).isEmpty();
    }

    @Test
    void withoutATransactionTheStepIsSubmittedAtOnce() {
        afterCommit.run("step", () -> { });

        assertThat(submitted).hasSize(1);
    }

    @Test
    void aFailingStepIsRetriedUpToTenAttemptsThenLoggedNotRethrown() {
        final AtomicInteger attempts = new AtomicInteger();
        afterCommit.run("step", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        submitted.get(0).run();

        assertThat(attempts).hasValue(AfterCommit.DEFAULT_MAX_ATTEMPTS).hasValue(10);
    }

    @Test
    void aStepThatSucceedsOnARetryIsNotRunAgain() {
        final AtomicInteger attempts = new AtomicInteger();
        afterCommit.run("step", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
        });

        submitted.get(0).run();

        assertThat(attempts).hasValue(3);
    }

    @Test
    void theAttemptsAreConfigurable() {
        final List<Runnable> queued = new ArrayList<>();
        final AtomicInteger attempts = new AtomicInteger();
        new AfterCommit(queued::add, 2).run("step", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        queued.get(0).run();

        assertThat(attempts).hasValue(2);
        assertThatThrownBy(() -> new AfterCommit(queued::add, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}

package uk.gov.moj.cpp.listing.command.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executors for the asynchronous follow-up steps of commands (MBD refactor P2.3.1, ADR 015).
 *
 * <p>{@code courtListExecutor}: one thread, FIFO. The JEE court-list saga ran its processor steps
 * one at a time, in outbox order (a single relay under an advisory lock): for a crown-courts
 * publication, every centre's "build and store" step ran before any centre's export, so each
 * export saw its CREST siblings' stored lists. A single-threaded FIFO executor gives the same
 * order: stage 1 of each request is queued at commit, and each stage 1 queues its export behind
 * the other requests' stage 1.
 */
@Configuration
public class CommandSupportConfiguration {

    public static final String COURT_LIST_EXECUTOR = "courtListExecutor";

    @Bean(name = COURT_LIST_EXECUTOR)
    public ThreadPoolTaskExecutor courtListExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("court-list-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    @Bean
    public AfterCommit courtListAfterCommit() {
        return new AfterCommit(courtListExecutor());
    }
}

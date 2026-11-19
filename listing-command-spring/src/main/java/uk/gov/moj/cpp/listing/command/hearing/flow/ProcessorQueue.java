package uk.gov.moj.cpp.listing.command.hearing.flow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Runs the processor steps of a command in the order the Phase 1 JEE relay ran them (MBD refactor
 * P2.3.2, ADR 015), inside the command's transaction.
 *
 * <p>JEE: the command's events reached the processor one at a time, in outbox order; a follow-up
 * command the processor sent ran in-process at once (P1g), its own events going to the end of the
 * outbox. So follow-up <em>commands</em> run depth-first, inside the processor step that sends
 * them, while <em>processor steps</em> run breadth-first, after every step already queued. Here:
 * the outermost command ({@link #command}) runs, then drains the queue; a follow-up command called
 * from a step only queues its steps behind the others.
 *
 * <p>One queue per thread; commands never cross threads.
 */
public class ProcessorQueue {

    private static final ThreadLocal<Deque<Runnable>> QUEUE = new ThreadLocal<>();

    /** Runs {@code command}; if it is the outermost one on this thread, then runs every queued step. */
    public <T> T command(final Supplier<T> command) {
        if (QUEUE.get() != null) {
            return command.get();
        }
        final Deque<Runnable> queue = new ArrayDeque<>();
        QUEUE.set(queue);
        try {
            final T result = command.get();
            Runnable step;
            while ((step = queue.poll()) != null) {
                step.run();
            }
            return result;
        } finally {
            QUEUE.remove();
        }
    }

    public void command(final Runnable command) {
        command(() -> {
            command.run();
            return null;
        });
    }

    /** Queues a processor step (runs it at once when no command is running, e.g. in a unit test). */
    public void enqueue(final Runnable step) {
        final Deque<Runnable> queue = QUEUE.get();
        if (queue == null) {
            step.run();
        } else {
            queue.add(step);
        }
    }
}

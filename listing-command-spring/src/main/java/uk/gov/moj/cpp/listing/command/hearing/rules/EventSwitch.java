package uk.gov.moj.cpp.listing.command.hearing.rules;

import java.util.function.Consumer;

/**
 * The framework's {@code EventSwitcher} ({@code match(event).with(when(X.class).apply(handler), …)}),
 * for the ported {@link HearingRules} state changes (MBD refactor P2.3.2, ADR 015): the first case
 * whose class the event is an instance of runs, and the event is returned unchanged.
 */
public final class EventSwitch {

    private EventSwitch() {
    }

    public static Matcher match(final Object event) {
        return new Matcher(event);
    }

    public static <T> When<T> when(final Class<T> eventClass) {
        return new When<>(eventClass);
    }

    public static Case otherwiseDoNothing() {
        return event -> true;
    }

    @FunctionalInterface
    public interface Case {
        /** @return whether this case handled {@code event} */
        boolean tryApply(Object event);
    }

    public static final class When<T> {
        private final Class<T> eventClass;

        private When(final Class<T> eventClass) {
            this.eventClass = eventClass;
        }

        public Case apply(final Consumer<T> handler) {
            return event -> {
                if (eventClass.isInstance(event)) {
                    handler.accept(eventClass.cast(event));
                    return true;
                }
                return false;
            };
        }
    }

    public static final class Matcher {
        private final Object event;

        private Matcher(final Object event) {
            this.event = event;
        }

        public Object with(final Case... cases) {
            for (final Case c : cases) {
                if (c.tryApply(event)) {
                    return event;
                }
            }
            throw new IllegalArgumentException("No case for event " + event.getClass().getName());
        }
    }
}

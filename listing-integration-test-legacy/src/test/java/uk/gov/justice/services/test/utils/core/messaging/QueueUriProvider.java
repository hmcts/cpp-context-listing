package uk.gov.justice.services.test.utils.core.messaging;

import uk.gov.moj.cpp.listing.legacyharness.LegacyStack;

import java.util.List;

/**
 * Legacy IT harness override (MBD refactor P2.3.0, §9: Artemis connection). Shadows the framework
 * test-utils class of the same name ({@code tcp://<host>:61616}, anonymous): the compose Artemis
 * listens on another host port and requires the IT broker's user ({@link LegacyStack#artemisUrl()}).
 * The port argument of the original API is ignored: there is one broker.
 */
public class QueueUriProvider {

    public String getQueueUri() {
        return LegacyStack.artemisUrl();
    }

    public String getQueueUri(final int port) {
        return LegacyStack.artemisUrl();
    }

    public List<String> getArtemisQueueUri() {
        return List.of(LegacyStack.artemisUrl());
    }

    public static String queueUri(final int port) {
        return LegacyStack.artemisUrl();
    }

    public static String queueUri() {
        return LegacyStack.artemisUrl();
    }

    public static List<String> artemisQueueUri() {
        return List.of(LegacyStack.artemisUrl());
    }
}

package uk.gov.justice.services.test.utils.core.http;

import uk.gov.moj.cpp.listing.legacyharness.LegacyStack;

/**
 * Legacy IT harness override (MBD refactor P2.3.0, §9: base URI and ports). Shadows the framework
 * test-utils class of the same name, which returned {@code http://<INTEGRATION_HOST_KEY>:8080}: the
 * Spring stack is reached through the legacy gateway on its own port ({@link LegacyStack}).
 */
public class BaseUriProvider {

    public static String getBaseUri() {
        return LegacyStack.baseUri();
    }
}

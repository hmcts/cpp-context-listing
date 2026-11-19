package uk.gov.moj.cpp.listing.legacyharness;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Points the legacy suite's static WireMock client at the compose WireMock container (MBD refactor
 * P2.3.0, §9: "WireMock moved to its own container"). Registered for every test class through JUnit
 * extension auto-detection ({@code junit-platform.properties}, {@code META-INF/services}), so no
 * scenario changes.
 *
 * <p>The legacy stub helpers configure the static client for {@code <host>:8080} in their static
 * initialisers ({@code CourtSchedulerServiceStub}; {@code WireMockStubUtils} is overridden). This
 * extension first forces those initialisers to run, then re-points the client: the static client
 * is per thread, and the tests and their {@code @BeforeEach} methods run on the thread that runs
 * these callbacks.
 */
public class LegacyHarnessExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final String[] STUB_CLASSES_CONFIGURING_WIREMOCK = {
            "uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub",
            "uk.gov.moj.cpp.listing.utils.WireMockStubUtils"
    };

    @Override
    public void beforeAll(final ExtensionContext context) {
        pointWireMockAtTheStack();
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        pointWireMockAtTheStack();
    }

    static void pointWireMockAtTheStack() {
        for (final String stubClass : STUB_CLASSES_CONFIGURING_WIREMOCK) {
            try {
                Class.forName(stubClass, true, LegacyHarnessExtension.class.getClassLoader());
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("Legacy stub class not on the test classpath: " + stubClass, e);
            }
        }
        WireMock.configureFor(LegacyStack.wireMockHost(), LegacyStack.wireMockPort());
    }
}

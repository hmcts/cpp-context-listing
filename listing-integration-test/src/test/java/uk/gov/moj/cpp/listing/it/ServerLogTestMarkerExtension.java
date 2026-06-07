package uk.gov.moj.cpp.listing.it;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Appends test lifecycle marker lines directly to the Wildfly server.log so that every
 * ERROR/WARN in the log can be attributed to the exact test that was executing.
 *
 * <p>The test JVM and Wildfly are separate processes, but Wildfly's log directory is
 * bind-mounted on the host ({@code $CPP_DOCKER_DIR/containers/wildfly/log/server.log}),
 * so this extension simply appends to the same file. Appends are single {@code write()}
 * calls in {@code O_APPEND} mode, which Linux interleaves safely with Wildfly's own writes.</p>
 *
 * <p>Markers produced (timestamp format matches Wildfly's own, UTC):</p>
 * <pre>
 * ==== [IT-CLASS-START] CrownUpdateHearingMultidayIT ====
 * ---- [TEST-START] CrownUpdateHearingMultidayIT.shouldExtendHearing ----
 * ---- [TEST-END][SUCCESS] CrownUpdateHearingMultidayIT.shouldExtendHearing ----
 * ---- [TEST-START][EXPECTED-ERRORS] CrownUpdateHearingMultidayIT.shouldReturn422... -- expects: courtscheduler 422 NO_AVAILABILITY ----
 * ---- [TEST-END][SUCCESS][EXPECTED-ERRORS] CrownUpdateHearingMultidayIT.shouldReturn422... ----
 * ==== [IT-CLASS-END] CrownUpdateHearingMultidayIT ====
 * </pre>
 *
 * <p>Triage rules:</p>
 * <ul>
 *   <li>ERROR/WARN between a [TEST-START] <b>without</b> [EXPECTED-ERRORS] and its
 *       [TEST-END] = genuine problem in that test's window — investigate.</li>
 *   <li>ERROR/WARN inside an [EXPECTED-ERRORS] window = deliberate negative scenario
 *       (see the {@link ExpectedServerErrors} reason text on the marker) — ignore.</li>
 *   <li>ERROR/WARN <b>between</b> a [TEST-END] and the next [TEST-START] = async straggler
 *       (in-flight JMS/event-processor work outlasting its test) — a missing await/drain
 *       in the test that just ended, or the known teardown race.</li>
 * </ul>
 *
 * <p>The [TEST-START] marker is written before {@code AbstractIT.setUp()} (extension
 * callbacks precede {@code @BeforeEach} methods) and [TEST-END] after {@code @AfterEach},
 * so setup/teardown errors are attributed to the right test as well.</p>
 *
 * <p>Resolution of the log path: {@code -Dserver.log.path} override, then
 * {@code $CPP_DOCKER_DIR}, then the conventional {@code ~/gitrepos/cpp-developers-docker}
 * location. If the directory does not exist (e.g. tests running against a remote stack),
 * the extension is a silent no-op — it must never fail a test.</p>
 */
public class ServerLogTestMarkerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, TestWatcher {

    private static final DateTimeFormatter LOG_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static final Path SERVER_LOG = resolveServerLogPath();

    @Override
    public void beforeAll(final ExtensionContext context) {
        writeMarker("==== [IT-CLASS-START] " + context.getRequiredTestClass().getSimpleName() + " ====");
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        writeMarker("==== [IT-CLASS-END] " + context.getRequiredTestClass().getSimpleName() + " ====");
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        final Optional<ExpectedServerErrors> expected = findExpectedServerErrors(context);
        final String tag = expected.isPresent() ? "[TEST-START][EXPECTED-ERRORS]" : "[TEST-START]";
        final String reason = expected.map(annotation -> " -- expects: " + annotation.value()).orElse("");
        writeMarker("---- " + tag + " " + testName(context) + reason + " ----");
        rememberWindowStartOffset(context);
    }

    /**
     * Scans this test's server.log window for unexpected ERROR/WARN lines: anything that is not
     * (a) sanctioned by {@link ExpectedServerErrors} or (b) a documented framework-race floor
     * signature. Findings are reported in the end-of-run summary
     * ({@link ServerLogUnexpectedErrorSummary}); with {@code -Dserver.log.failOnUnexpectedErrors=true}
     * the owning test fails instead (off by default — the floor races land in arbitrary windows
     * and would make runs flaky).
     */
    @Override
    public void afterEach(final ExtensionContext context) {
        if (findExpectedServerErrors(context).isPresent()) {
            return; // sanctioned negative-scenario window
        }
        final long startOffset = windowStartOffset(context);
        if (startOffset < 0) {
            return; // marker file unavailable on this machine
        }
        final String window = readLogWindow(startOffset);
        final List<String> unexpected = findUnexpectedErrorLines(window);
        if (!unexpected.isEmpty()) {
            ServerLogUnexpectedErrorSummary.record(testName(context), unexpected);
            if (FAIL_ON_UNEXPECTED_ERRORS) {
                throw new AssertionError("Unexpected server.log ERROR/WARN lines during this test "
                        + "(see markers in server.log; annotate with @ExpectedServerErrors only if deliberate):\n  "
                        + String.join("\n  ", unexpected));
            }
        }
    }

    @Override
    public void testSuccessful(final ExtensionContext context) {
        writeTestEnd(context, "SUCCESS");
    }

    @Override
    public void testFailed(final ExtensionContext context, final Throwable cause) {
        writeTestEnd(context, "FAILED");
    }

    @Override
    public void testAborted(final ExtensionContext context, final Throwable cause) {
        writeTestEnd(context, "ABORTED");
    }

    @Override
    public void testDisabled(final ExtensionContext context, final Optional<String> reason) {
        writeMarker("---- [TEST-DISABLED] " + testName(context) + " ----");
    }

    private void writeTestEnd(final ExtensionContext context, final String outcome) {
        final String expectedTag = findExpectedServerErrors(context).isPresent() ? "[EXPECTED-ERRORS]" : "";
        writeMarker("---- [TEST-END][" + outcome + "]" + expectedTag + " " + testName(context) + " ----");
    }

    // ---- unexpected-error scanning ------------------------------------------------------------

    private static final boolean FAIL_ON_UNEXPECTED_ERRORS = Boolean.getBoolean("server.log.failOnUnexpectedErrors");

    /** Extra allowlist regex appended via -Dserver.log.error.allowlist=<regex> (optional). */
    private static final String CUSTOM_ALLOWLIST = System.getProperty("server.log.error.allowlist", "");

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ServerLogTestMarkerExtension.class);

    /**
     * Documented framework-race floor (see memory/triage doc): async tail of test N crossing
     * test N+1's reset()/truncate. Self-heals on redelivery, no DLQ, also present on main.
     */
    private static final java.util.regex.Pattern FLOOR_NOISE = java.util.regex.Pattern.compile(
            "No stream not found"                 // EVENT_LISTENER stream_status select-for-update race
                    + "|Failed to find Event with id"     // AsynchronousPrePublisher vs event-store truncate
                    + "|AMQ212009: resetting session");   // Artemis companion WARN of any rolled-back delivery

    private void rememberWindowStartOffset(final ExtensionContext context) {
        long offset = -1;
        try {
            if (Files.isRegularFile(SERVER_LOG)) {
                offset = Files.size(SERVER_LOG);
            }
        } catch (final IOException ignored) {
            // scanning is best-effort; never fail a test over it
        }
        context.getStore(NAMESPACE).put("windowStart", offset);
    }

    private long windowStartOffset(final ExtensionContext context) {
        final Long offset = context.getStore(NAMESPACE).get("windowStart", Long.class);
        return offset == null ? -1 : offset;
    }

    private static String readLogWindow(final long startOffset) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(SERVER_LOG.toFile(), "r")) {
            final long end = raf.length();
            if (end <= startOffset) {
                return "";
            }
            final byte[] bytes = new byte[(int) Math.min(end - startOffset, 64L * 1024 * 1024)];
            raf.seek(startOffset);
            raf.readFully(bytes);
            return new String(bytes, UTF_8);
        } catch (final IOException e) {
            return "";
        }
    }

    private static List<String> findUnexpectedErrorLines(final String window) {
        // Companion suppression: a stream-status race manifests as a WFLYEJB0034+AMQ154004 pair
        // whose MAIN line only says "Failed to process event..." — the discriminating cause
        // ("No stream not found") is in the stack lines below. Suppress those companions only
        // when the race signature is actually present in this window, so a genuinely new
        // listener failure still surfaces.
        final boolean streamRaceInWindow = window.contains("No stream not found");
        final List<String> unexpected = new java.util.ArrayList<>();
        for (String line : window.split("\n")) {
            line = line.replaceAll("\\[[0-9;]*m", "");
            if (!line.contains(" ERROR ") && !line.contains(" WARN ")) {
                continue;
            }
            if (line.contains("[TEST-") || line.contains("[IT-CLASS")) {
                continue; // our own markers (reason text may contain the words ERROR/WARN)
            }
            if (FLOOR_NOISE.matcher(line).find()) {
                continue;
            }
            if (streamRaceInWindow && line.contains("Failed to process event")) {
                continue;
            }
            if (!CUSTOM_ALLOWLIST.isBlank() && line.matches(".*(" + CUSTOM_ALLOWLIST + ").*")) {
                continue;
            }
            unexpected.add(line);
        }
        return unexpected;
    }

    private static Optional<ExpectedServerErrors> findExpectedServerErrors(final ExtensionContext context) {
        final Optional<ExpectedServerErrors> onMethod = context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, ExpectedServerErrors.class));
        if (onMethod.isPresent()) {
            return onMethod;
        }
        return context.getTestClass()
                .flatMap(testClass -> AnnotationSupport.findAnnotation(testClass, ExpectedServerErrors.class));
    }

    private static String testName(final ExtensionContext context) {
        final String className = context.getTestClass().map(Class::getSimpleName).orElse("?");
        final String methodName = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("?");
        final String displayName = context.getDisplayName();
        // Parameterized/display-named tests: include the display name for disambiguation
        if (!displayName.equals(methodName + "()") && !displayName.equals(methodName)) {
            return className + "." + methodName + " (" + displayName + ")";
        }
        return className + "." + methodName;
    }

    private static void writeMarker(final String text) {
        if (!Files.isDirectory(SERVER_LOG.getParent())) {
            return; // wildfly log dir not mounted on this machine — markers are a local-stack aid only
        }
        // Leading newline guards against landing mid-line if Wildfly is mid-write; single
        // write() call keeps the marker atomic w.r.t. Wildfly's own appends.
        final String line = "\n" + LOG_TIMESTAMP.format(Instant.now()) + " " + text + "\n";
        try {
            Files.write(SERVER_LOG, line.getBytes(UTF_8), CREATE, WRITE, APPEND);
        } catch (final IOException ignored) {
            // never fail a test because a log marker could not be written
        }
    }

    private static Path resolveServerLogPath() {
        final String override = System.getProperty("server.log.path");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        final String dockerDir = System.getenv("CPP_DOCKER_DIR");
        if (dockerDir != null && !dockerDir.isBlank()) {
            return Paths.get(dockerDir, "containers", "wildfly", "log", "server.log");
        }
        return Paths.get(System.getProperty("user.home"),
                "gitrepos", "cpp-developers-docker", "containers", "wildfly", "log", "server.log");
    }
}

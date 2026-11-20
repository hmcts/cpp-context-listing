package uk.gov.moj.cpp.listing.it;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Collects unexpected server.log ERROR/WARN lines found by {@link ServerLogTestMarkerExtension}
 * during each test's window and prints a consolidated summary at the END of the test run
 * (registered via META-INF/services/org.junit.platform.launcher.TestExecutionListener, so the
 * summary lands in the failsafe console output, after the last test class).
 *
 * <p>The summary is also written to {@code target/unexpected-server-errors.txt} so CI archives it
 * even when console output is truncated.</p>
 *
 * <p>An "unexpected" line is any ERROR/WARN inside a test's [TEST-START]→test-end window that is
 * NOT (a) inside an {@link ExpectedServerErrors} window, and NOT (b) one of the documented
 * framework-race floor signatures (see {@link ServerLogTestMarkerExtension#isFloorNoise}).</p>
 */
public class ServerLogUnexpectedErrorSummary implements TestExecutionListener {

    /** One finding = one test window that contained unexpected ERROR/WARN lines. */
    public static final class Finding {
        final String testName;
        final List<String> lines;

        Finding(final String testName, final List<String> lines) {
            this.testName = testName;
            this.lines = lines;
        }
    }

    private static final List<Finding> FINDINGS = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LINE_LENGTH_IN_SUMMARY = 300;

    public static void record(final String testName, final List<String> unexpectedLines) {
        FINDINGS.add(new Finding(testName, unexpectedLines));
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        final StringBuilder summary = new StringBuilder();
        summary.append("\n=================== SERVER.LOG UNEXPECTED ERROR SUMMARY ===================\n");
        if (FINDINGS.isEmpty()) {
            summary.append("server.log CLEAN: no unexpected ERROR/WARN lines outside [EXPECTED-ERRORS] windows.\n");
        } else {
            summary.append(FINDINGS.size()).append(" test window(s) contained unexpected server.log ERROR/WARN lines\n")
                    .append("(framework-race floor signatures and @ExpectedServerErrors windows already filtered):\n");
            synchronized (FINDINGS) {
                for (final Finding finding : FINDINGS) {
                    summary.append("\n  ").append(finding.testName).append("\n");
                    for (final String line : finding.lines) {
                        summary.append("    ")
                                .append(line, 0, Math.min(line.length(), MAX_LINE_LENGTH_IN_SUMMARY))
                                .append("\n");
                    }
                }
            }
            summary.append("\nInvestigate these before merging; happy-path tests must keep server.log clean.\n")
                    .append("Re-run with -Dserver.log.failOnUnexpectedErrors=true to fail the owning test instead.\n");
        }
        summary.append("============================================================================\n");

        System.out.println(summary);
        writeReportFile(summary.toString());
    }

    private void writeReportFile(final String summary) {
        try {
            final Path reportFile = Paths.get("target", "unexpected-server-errors.txt");
            Files.createDirectories(reportFile.getParent());
            Files.write(reportFile, summary.getBytes(UTF_8));
        } catch (final IOException ignored) {
            // the console summary is the primary output; never fail the run over the report file
        }
    }
}

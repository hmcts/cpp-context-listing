package uk.gov.moj.cpp.listing.it;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a negative-scenario test (or a whole test class) that DELIBERATELY provokes
 * server-side ERROR/WARN lines in the Wildfly server.log (e.g. by stubbing a downstream
 * service to return 422/500 and asserting the fail-safe behaviour).
 *
 * <p>{@link ServerLogTestMarkerExtension} reads this annotation and stamps the test's
 * [TEST-START]/[TEST-END] markers in server.log with an extra [EXPECTED-ERRORS] tag plus
 * the reason text, so anyone triaging the log knows errors inside that window are by
 * design and need no investigation.</p>
 *
 * <p>Triage rule this enables: <b>any ERROR/WARN between a [TEST-START] marker WITHOUT
 * [EXPECTED-ERRORS] and its [TEST-END] is a genuine problem.</b> Happy-path tests must
 * keep the log clean; only annotated negative scenarios may emit errors.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExpectedServerErrors {

    /**
     * Short description of the exact ERROR/WARN lines this scenario is expected to produce,
     * so a log reader can match them without opening the test source.
     */
    String value();
}

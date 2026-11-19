package uk.gov.moj.cpp.listing.envelope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker on controller methods (or classes) whose response body must be left exactly as returned:
 * no {@code _metadata} envelope and no {@code CPPID} header (e.g. the {@code text/csv} hearing
 * report). Ported from courtscheduler; TYPE is allowed because the advice already honours it.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipEnvelope {
}

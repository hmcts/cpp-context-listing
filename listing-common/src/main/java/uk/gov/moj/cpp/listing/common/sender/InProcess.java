package uk.gov.moj.cpp.listing.common.sender;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Selects {@link InProcessSender} (MBD refactor P1g): {@code @Inject @InProcess Sender sender}
 * runs listing's own commands in-VM instead of sending them to {@code listing.handler.command}.
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE})
public @interface InProcess {
}

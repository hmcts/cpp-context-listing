package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.CommandRequests.metadata;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.payload;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/** Shared bits of the Hearing command controllers (MBD refactor P2.3.2). */
final class HearingCommandRequests {

    private HearingCommandRequests() {
    }

    /** The command as the framework REST adapter built it: metadata named after the action, path parameters merged into the body. */
    static Envelope command(final HttpServletRequest request, final String actionName, final Map<String, Object> body,
                            final Map<String, String> pathParameters) {
        return new Envelope(metadata(request, actionName), payload(body, pathParameters));
    }
}

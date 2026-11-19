package uk.gov.moj.cpp.listing.controllers;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * What the framework's REST adapter ({@code RestEnvelopeBuilder}) did with a command request, for
 * the P2.3 command controllers (MBD refactor, ADR 014): the command's metadata and its payload.
 *
 * <p>Metadata: a new {@code id}, the action name as {@code name} (e.g.
 * {@code listing.command.create-listing-note}), and the {@code CPPCLIENTCORRELATIONID},
 * {@code CJSCPPUID}, {@code CPPSID} and {@code CPPCAUSATION} headers as {@code correlation.client},
 * {@code context.user}, {@code context.session} and {@code causation}. No {@code createdAt}, as the
 * framework. The public messages a command writes take their metadata from this ({@code EventOutbox#envelopeFor}).
 *
 * <p>Payload: the JSON body with the path parameters added (a path parameter wins over a body field
 * of the same name), as the framework merged them.
 */
final class CommandRequests {

    static final String USER_ID = "CJSCPPUID";
    static final String SESSION_ID = "CPPSID";
    static final String CLIENT_CORRELATION_ID = "CPPCLIENTCORRELATIONID";
    static final String CAUSATION = "CPPCAUSATION";

    private CommandRequests() {
    }

    static Metadata metadata(final HttpServletRequest request, final String actionName) {
        final Metadata.Builder metadata = Metadata.builder()
                .withId(randomUUID())
                .withName(actionName);
        header(request, CLIENT_CORRELATION_ID).ifPresent(metadata::withClientCorrelationId);
        header(request, USER_ID).ifPresent(metadata::withUserId);
        header(request, SESSION_ID).ifPresent(metadata::withSessionId);
        header(request, CAUSATION).ifPresent(causation -> metadata.withCausation(causation(causation)));
        return metadata.build();
    }

    static JsonObject payload(final Map<String, Object> body, final Map<String, String> pathParameters) {
        final JsonObjectBuilder payload = Json.createObjectBuilder(body == null ? Map.of() : body);
        pathParameters.forEach(payload::add);
        return payload.build();
    }

    static ResponseEntity<Void> accepted() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /** {@code application/vnd.listing.command.edit-listing-note+json} → {@code listing.command.edit-listing-note}. */
    static String actionName(final HttpServletRequest request) {
        final String contentType = request.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("No Content-Type");
        }
        final String mediaType = contentType.split(";", 2)[0].trim();
        if (!mediaType.startsWith("application/vnd.") || !mediaType.endsWith("+json")) {
            throw new IllegalArgumentException("Not a vendor JSON media type: " + mediaType);
        }
        return mediaType.substring("application/vnd.".length(), mediaType.length() - "+json".length());
    }

    private static java.util.Optional<String> header(final HttpServletRequest request, final String name) {
        return java.util.Optional.ofNullable(request.getHeader(name)).filter(value -> !value.isBlank());
    }

    private static List<UUID> causation(final String header) {
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(UUID::fromString)
                .toList();
    }
}

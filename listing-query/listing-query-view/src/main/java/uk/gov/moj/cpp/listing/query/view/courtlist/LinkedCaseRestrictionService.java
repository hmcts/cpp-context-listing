package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.isNull;

import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Keeps a restricted case from being named under another case's hearing. A hearing's case can carry the cases it is
 * linked to (case id and URN). A linked case that is itself restricted from the court list, for example a youth
 * case, must not show its URN there, in the same way that the case would not appear on the list itself.
 * <p>
 * A linked case is restricted when a hearing that lists it marks it {@code restrictFromCourtList}. A linked case that
 * is not listed in this context has no restriction recorded here, so it is kept.
 */
@ApplicationScoped
public class LinkedCaseRestrictionService {

    private static final String HEARINGS = "hearings";
    private static final String LISTED_CASES = "listedCases";
    private static final String LINKED_CASES = "linkedCases";
    private static final String CASE_ID = "caseId";
    private static final String ID = "id";
    private static final String RESTRICT_FROM_COURT_LIST = "restrictFromCourtList";

    @Inject
    private HearingRepository hearingRepository;

    /**
     * @param rangeSearchPayload the range search response, with its {@code hearings} array
     * @return the same payload with restricted cases removed from every {@code linkedCases} list; the payload itself
     * when it has no linked cases or none of them is restricted
     */
    public JsonObject withoutRestrictedLinkedCases(final JsonObject rangeSearchPayload) {
        final JsonArray hearings = rangeSearchPayload.getJsonArray(HEARINGS);
        if (isNull(hearings)) {
            return rangeSearchPayload;
        }
        final Set<UUID> linkedCaseIds = linkedCaseIds(hearings);
        if (linkedCaseIds.isEmpty()) {
            return rangeSearchPayload;
        }
        final Set<UUID> restricted = restrictedCaseIds(linkedCaseIds);
        if (restricted.isEmpty()) {
            return rangeSearchPayload;
        }

        final JsonArrayBuilder filteredHearings = JsonObjects.createArrayBuilder();
        hearings.getValuesAs(JsonObject.class).forEach(hearing -> filteredHearings.add(withoutRestricted(hearing, restricted)));
        return JsonObjects.createObjectBuilder(rangeSearchPayload).add(HEARINGS, filteredHearings).build();
    }

    private static Set<UUID> linkedCaseIds(final JsonArray hearings) {
        final Set<UUID> ids = new HashSet<>();
        hearings.getValuesAs(JsonObject.class).forEach(hearing -> listedCases(hearing).forEach(listedCase ->
                linkedCases(listedCase).forEach(linked -> parse(linked.getString(CASE_ID, null)).ifPresent(ids::add))));
        return ids;
    }

    private Set<UUID> restrictedCaseIds(final Set<UUID> caseIds) {
        final Set<UUID> restricted = new HashSet<>();
        final Set<String> caseIdTexts = caseIds.stream().map(UUID::toString).collect(Collectors.toSet());
        for (final Hearing hearing : hearingRepository.findHearingsListingCases(caseIdTexts)) {
            final JsonNode listedCases = hearing.getProperties() == null ? null : hearing.getProperties().path(LISTED_CASES);
            if (listedCases == null || !listedCases.isArray()) {
                continue;
            }
            for (final JsonNode listedCase : listedCases) {
                if (listedCase.path(RESTRICT_FROM_COURT_LIST).asBoolean(false)) {
                    parse(listedCase.path(ID).asText(null)).filter(caseIds::contains).ifPresent(restricted::add);
                }
            }
        }
        return restricted;
    }

    private static JsonObject withoutRestricted(final JsonObject hearing, final Set<UUID> restricted) {
        final JsonArray listedCases = hearing.getJsonArray(LISTED_CASES);
        if (isNull(listedCases)) {
            return hearing;
        }
        final JsonArrayBuilder filteredListedCases = JsonObjects.createArrayBuilder();
        listedCases.getValuesAs(JsonObject.class).forEach(listedCase -> filteredListedCases.add(withoutRestrictedLinkedCases(listedCase, restricted)));
        return JsonObjects.createObjectBuilder(hearing).add(LISTED_CASES, filteredListedCases).build();
    }

    private static JsonObject withoutRestrictedLinkedCases(final JsonObject listedCase, final Set<UUID> restricted) {
        final JsonArray linkedCases = listedCase.getJsonArray(LINKED_CASES);
        if (isNull(linkedCases)) {
            return listedCase;
        }
        final JsonArrayBuilder kept = JsonObjects.createArrayBuilder();
        linkedCases.getValuesAs(JsonObject.class).stream()
                .filter(linked -> parse(linked.getString(CASE_ID, null)).map(id -> !restricted.contains(id)).orElse(true))
                .forEach(kept::add);
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder(listedCase);
        return builder.add(LINKED_CASES, kept).build();
    }

    private static Iterable<JsonObject> listedCases(final JsonObject hearing) {
        final JsonArray listedCases = hearing.getJsonArray(LISTED_CASES);
        return isNull(listedCases) ? java.util.List.of() : listedCases.getValuesAs(JsonObject.class);
    }

    private static Iterable<JsonObject> linkedCases(final JsonObject listedCase) {
        final JsonArray linkedCases = listedCase.getJsonArray(LINKED_CASES);
        return isNull(linkedCases) ? java.util.List.of() : linkedCases.getValuesAs(JsonObject.class);
    }

    private static java.util.Optional<UUID> parse(final String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}

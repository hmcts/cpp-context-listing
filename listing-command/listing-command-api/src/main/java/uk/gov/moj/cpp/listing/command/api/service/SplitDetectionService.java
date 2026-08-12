package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects, BEFORE enrichment, that an update-hearing-for-listing command is the originating half
 * of a hearing SPLIT, and flags it via {@code splitHearing} so enrichment (CROWN and
 * MAGISTRATES alike) stays read-only for the original hearing (no listHearingInCourtSessions /
 * multiDaySearchAndBook / search-and-book / slot search under the original hearingId — those
 * writes move the original hearing's allocated_listings rows onto the split's new sessions in
 * courtscheduler). The flag value follows the existing convention: "unallocated" when the
 * carved-out cases go unallocated (no room / week-commencing), "allocated" for splits onto
 * chosen session(s).
 *
 * <p>The handler only classifies SPLIT after enrichment ({@code ExtendHearingUtils.getOperationType}),
 * which is too late: the courtscheduler writes have already happened. This service mirrors that
 * classification using the same viewstore lookup the handler uses ({@code listing.search.hearing}):
 * the request's offences are a STRICT SUBSET of the stored hearing's offences, combined with the
 * same courtRoom / weekCommencing / allocated arms as getOperationType. It never unsets a
 * caller-provided splitHearing, and detection failures fail open (treated as not-a-split) so
 * non-split updates are unaffected.
 */
@ApplicationScoped
public class SplitDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitDetectionService.class);

    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String LISTED_CASES = "listedCases";
    private static final String DEFENDANTS = "defendants";
    private static final String OFFENCES = "offences";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String OFFENCE_ID = "offenceId";
    private static final String ID = "id";
    /** JSON field name on the stored (viewstore) hearing - the hearing's allocation state. */
    private static final String ALLOCATED = "allocated";
    /**
     * splitHearing flag values. SPLIT_UNALLOCATED matches the pre-existing convention the FE and
     * {@code ExtendHearingForHearingListener} use for splits whose carved-out cases go unallocated
     * (no court room / week-commencing); SPLIT_ALLOCATED marks a split onto chosen session(s).
     * SPLIT_ALLOCATED sharing the {@code ALLOCATED} literal is coincidence, not shared meaning.
     */
    private static final String SPLIT_UNALLOCATED = "unallocated";
    private static final String SPLIT_ALLOCATED = "allocated";

    @Inject
    private HearingLookupService hearingLookupService;

    public UpdateHearingForListing flagSplitIfDetected(final UpdateHearingForListing hearing,
                                                       final JsonObject rawPayload,
                                                       final JsonEnvelope envelope) {
        if (isNotBlank(hearing.getSplitHearing())) {
            return hearing;
        }
        final Optional<String> splitHearingValue;
        try {
            splitHearingValue = detectSplitHearingValue(hearing, rawPayload, envelope);
        } catch (final RuntimeException e) {
            LOGGER.warn("Split detection failed for hearingId {} - treating as non-split.", hearing.getHearingId(), e);
            return hearing;
        }
        if (splitHearingValue.isEmpty()) {
            return hearing;
        }
        LOGGER.info("Split detected for hearingId {} (request offences are a strict subset of the stored hearing's) - flagging splitHearing={} so enrichment stays read-only for the original hearing.",
                hearing.getHearingId(), splitHearingValue.get());
        return UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(hearing)
                .withSplitHearing(splitHearingValue.get())
                .build();
    }

    private Optional<String> detectSplitHearingValue(final UpdateHearingForListing hearing,
                                                     final JsonObject rawPayload,
                                                     final JsonEnvelope envelope) {
        final Set<String> requestOffences = extractRequestOffenceIds(rawPayload);
        if (requestOffences.isEmpty()) {
            return Optional.empty();
        }

        final Optional<JsonObject> storedHearing = hearingLookupService.findHearing(hearing.getHearingId(), envelope);
        if (storedHearing.isEmpty()) {
            return Optional.empty();
        }
        final Set<String> persistedOffences = extractStoredOffenceIds(storedHearing.get());

        // Mirrors ExtendHearingUtils.getOperationType's subset test: every requested offence is
        // already on the stored hearing AND at least one stored offence is NOT in the request
        // (the cases being carved out onto the new hearing).
        final boolean strictSubset = persistedOffences.containsAll(requestOffences)
                && requestOffences.size() < persistedOffences.size();
        if (!strictSubset) {
            LOGGER.debug("Not a split for hearingId {}: request offences ({}) are not a strict subset of stored offences ({}).",
                    hearing.getHearingId(), requestOffences.size(), persistedOffences.size());
            return Optional.empty();
        }

        // Mirrors getOperationType's two SPLIT arms; anything else (e.g. PARTIAL_ALLOCATION,
        // where the original hearing IS meant to be re-listed) stays unflagged.
        final UUID courtRoomId = resolveCourtRoomId(hearing);
        if (courtRoomId == null || nonNull(hearing.getWeekCommencingStartDate())) {
            return Optional.of(SPLIT_UNALLOCATED);
        }
        final boolean storedAllocated = storedHearing.get().getBoolean(ALLOCATED, false);
        // getOperationType evaluates this arm on the ENRICHED hearingDays, which for the
        // court-calendar CROWN "schedule-only" shape are seeded from nonDefaultDays during
        // enrichment. Pre-enrichment, that shape has EMPTY hearingDays and the session details on
        // nonDefaultDays - so days-present must consider both, or session-picked splits with a
        // room (arm 1 false) are never flagged and enrichment writes under the original hearingId.
        final boolean daysPresent = (nonNull(hearing.getHearingDays()) && !hearing.getHearingDays().isEmpty())
                || (nonNull(hearing.getNonDefaultDays()) && !hearing.getNonDefaultDays().isEmpty());
        if (daysPresent && storedAllocated) {
            return Optional.of(SPLIT_ALLOCATED);
        }
        // Split-shaped (strict offence subset) but no SPLIT arm matched - this is the
        // PARTIAL_ALLOCATION shape where re-listing the original hearing is intended. Logged at
        // INFO because it is rare and is the key diagnostic when a real split goes unflagged
        // (e.g. a jurisdiction-specific payload shape we haven't accounted for).
        LOGGER.info("Split-shaped update for hearingId {} NOT flagged: courtRoomId={}, weekCommencingStartDate={}, daysPresent={}, storedAllocated={} - treating as partial allocation.",
                hearing.getHearingId(), courtRoomId, hearing.getWeekCommencingStartDate(), daysPresent, storedAllocated);
        return Optional.empty();
    }

    /**
     * Mirrors the handler's getCourtRoomId: for CROWN with a selectedCourtCentre the room comes
     * from the selection, otherwise from the command-level field.
     */
    private static UUID resolveCourtRoomId(final UpdateHearingForListing hearing) {
        final SelectedCourtCentre selectedCourtCentre = hearing.getSelectedCourtCentre();
        if (nonNull(selectedCourtCentre)
                && JurisdictionType.CROWN.equals(hearing.getJurisdictionType())) {
            return selectedCourtCentre.getCourtRoomId();
        }
        return hearing.getCourtRoomId();
    }

    /**
     * Request shape: prosecutionCases[].defendants[].offences[].offenceId - defendants without a
     * defendantId are skipped, mirroring ExtendHearingUtils.buildRequestedCaseDefendantOffenceMap.
     */
    private static Set<String> extractRequestOffenceIds(final JsonObject rawPayload) {
        return extractOffenceIds(rawPayload.getJsonArray(PROSECUTION_CASES), SplitDetectionService::hasDefendantId, OFFENCE_ID);
    }

    /**
     * Stored (viewstore listing.search.hearing) shape: listedCases[].defendants[].offences[].id.
     */
    private static Set<String> extractStoredOffenceIds(final JsonObject storedHearing) {
        return extractOffenceIds(storedHearing.getJsonArray(LISTED_CASES), defendant -> true, ID);
    }

    private static Set<String> extractOffenceIds(final JsonArray cases,
                                                 final Predicate<JsonObject> defendantFilter,
                                                 final String offenceIdField) {
        return objectStream(cases)
                .flatMap(caseObject -> objectStream(caseObject.getJsonArray(DEFENDANTS)))
                .filter(defendantFilter)
                .flatMap(defendant -> objectStream(defendant.getJsonArray(OFFENCES)))
                .map(offence -> offence.getString(offenceIdField, null))
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private static Stream<JsonObject> objectStream(final JsonArray array) {
        return array == null ? Stream.empty() : array.getValuesAs(JsonObject.class).stream();
    }

    private static boolean hasDefendantId(final JsonObject defendant) {
        return defendant.containsKey(DEFENDANT_ID) && !defendant.isNull(DEFENDANT_ID);
    }
}

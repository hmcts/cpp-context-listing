package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.CommandRequests.accepted;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.actionName;
import static uk.gov.moj.cpp.listing.controllers.HearingCommandRequests.command;

import uk.gov.moj.cpp.listing.command.hearing.api.HearingCommandApi;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.openapi.api.HearingCommandsOpenApi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Hearing commands (MBD refactor P2.3.2, ADR 015): {@code POST /hearings},
 * {@code /hearings/update-hearings}, {@code /hearings/{hearingId}}, {@code /hearings/sequence} and
 * {@code /delete-hearing/{hearingId}}, dispatched on Content-Type where paths are shared. Each runs
 * synchronously in the request (D5) and answers 202. P2.3.3 maps the remaining media types (next
 * hearings, split, duplicate and the four RAML-only Hearing commands); the envelope name is the
 * RAML {@code (mapping)} name, which differs from the media type for duplicate-unallocated-hearing,
 * next-hearings-v2 and related-hearing.
 */
@RestController
public class HearingCommandsApi implements HearingCommandsOpenApi {

    static final String CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    static final String RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    static final String DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING = "listing.delete-previous-hearings-and-create-next-hearing";
    static final String UPDATE_HEARINGS_FOR_LISTING = "listing.command.update-hearings-for-listing";
    static final String UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    static final String EXTEND_HEARING_FOR_HEARING = "listing.command.extend-hearing-for-hearing";
    static final String VACATE_TRIAL = "listing.command.vacate-trial";
    static final String MOVE_HEARING_TO_PAST_DATE = "listing.command.move-hearing-to-past-date";
    static final String CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING = "listing.command.change-court-room-for-multiday-hearing";
    static final String UPDATE_HEARING_ADD_CASE_BDF = "listing.update-hearing-add-case-bdf";
    static final String SEQUENCE_HEARINGS = "listing.command.sequence-hearings";
    static final String DELETE_HEARING = "listing.delete-hearing";
    // P2.3.3: media type (Content-Type action) → the RAML (mapping) name the envelope carries.
    static final String SPLIT_HEARING = "listing.command.split-hearing";
    static final String UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";
    static final String DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    static final String ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    static final String UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    static final String DUPLICATE_UNALLOCATED_HEARING_MEDIA_TYPE = "listing.duplicate-unallocated-hearing";
    static final String MARK_UNALLOCATED_HEARING_AS_DUPLICATE = "listing.mark-unallocated-hearing-as-duplicate";
    static final String NEXT_HEARINGS_V2_MEDIA_TYPE = "listing.next-hearings-v2";
    static final String LIST_NEXT_HEARINGS_V2 = "listing.list-next-hearings-v2";
    static final String LIST_UNSCHEDULED_NEXT_HEARINGS = "listing.list-unscheduled-next-hearings";
    static final String RELATED_HEARING_MEDIA_TYPE = "listing.related-hearing";
    static final String UPDATE_RELATED_HEARING = "listing.update-related-hearing";
    static final String DELETE_NEXT_HEARINGS = "listing.delete-next-hearings";

    private final HearingCommandApi api;
    private final HttpServletRequest request;

    public HearingCommandsApi(final HearingCommandApi api, final HttpServletRequest request) {
        this.api = api;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> postHearings(final Map<String, Object> requestBody) {
        final String action = actionName(request);
        final Envelope command = command(request, action, requestBody, Map.of());
        switch (action) {
            case CHANGE_JUDICIARY_FOR_HEARINGS -> api.handleChangeJudiciaryForHearings(command);
            case RESTRICT_COURT_LIST -> api.handleRestrictCourtList(command);
            case DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING -> api.handleDeletePreviousHearingsAndCreateNextHearing(command);
            default -> throw new IllegalStateException("Unexpected media type for /hearings: " + action);
        }
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postUpdateHearings(final Map<String, Object> requestBody) {
        api.handleUpdateHearingsForListing(command(request, UPDATE_HEARINGS_FOR_LISTING, requestBody, Map.of()));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postHearing(final String hearingId, final Map<String, Object> requestBody) {
        final String mediaType = actionName(request);
        final Map<String, String> path = Map.of("hearingId", hearingId);
        if (requestBody == null && !DUPLICATE_UNALLOCATED_HEARING_MEDIA_TYPE.equals(mediaType)) {
            // The body is optional in the spec only for duplicate-unallocated-hearing (no body in RAML).
            throw new HttpMessageNotReadableException("Required request body is missing", (HttpInputMessage) null);
        }
        switch (mediaType) {
            case UPDATE_HEARING_FOR_LISTING -> api.handleUpdateHearingForListing(command(request, mediaType, requestBody, path));
            case EXTEND_HEARING_FOR_HEARING -> api.handleExtendHearingForHearing(command(request, mediaType, requestBody, path));
            case VACATE_TRIAL -> api.handleVacateTrial(command(request, mediaType, requestBody, path));
            case MOVE_HEARING_TO_PAST_DATE -> api.handleMoveHearingToPastDate(command(request, mediaType, requestBody, path));
            case CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING -> api.handleChangeCourtRoomForMultidayHearing(command(request, mediaType, requestBody, path));
            case UPDATE_HEARING_ADD_CASE_BDF -> api.handleUpdateHearingAddCaseBdf(command(request, mediaType, requestBody, path));
            case SPLIT_HEARING -> api.handleSplitHearing(command(request, mediaType, requestBody, path));
            // No JEE command-API @Handles: the framework's generated @Remote COMMAND_API handler
            // passed them unchanged to listing.handler.command (P2.3.3: the command service).
            case UPDATE_DEFENDANTS_FOR_HEARING,
                 DELETE_OFFENCES_FOR_HEARING,
                 ADD_OFFENCES_FOR_HEARING,
                 UPDATE_OFFENCES_FOR_HEARING -> api.handlePassThroughHearingCommand(command(request, mediaType, requestBody, path));
            // The RAML (mapping) names differ from the media types for these.
            case DUPLICATE_UNALLOCATED_HEARING_MEDIA_TYPE ->
                    api.handleMarkUnallocatedHearingAsDuplicate(command(request, MARK_UNALLOCATED_HEARING_AS_DUPLICATE, null, path));
            case NEXT_HEARINGS_V2_MEDIA_TYPE -> api.listNextHearings(command(request, LIST_NEXT_HEARINGS_V2, requestBody, path));
            case LIST_UNSCHEDULED_NEXT_HEARINGS -> api.handleListUnscheduledNextCourtHearings(command(request, mediaType, requestBody, path));
            case RELATED_HEARING_MEDIA_TYPE -> api.updateRelatedHearing(command(request, UPDATE_RELATED_HEARING, requestBody, path));
            case DELETE_NEXT_HEARINGS -> api.handleDeleteNextHearings(command(request, mediaType, requestBody, path));
            default -> throw new IllegalStateException("Unexpected media type for /hearings/{hearingId}: " + mediaType);
        }
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postSequenceHearings(final Map<String, Object> requestBody) {
        api.handleSequenceHearings(command(request, SEQUENCE_HEARINGS, requestBody, Map.of()));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postDeleteHearing(final String hearingId, final Map<String, Object> requestBody) {
        api.handleDeleteHearing(command(request, DELETE_HEARING, requestBody, Map.of("hearingId", hearingId)));
        return accepted();
    }
}

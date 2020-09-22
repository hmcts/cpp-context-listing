package uk.gov.moj.cpp.listing.steps;

import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.listing.it.AbstractIT;

import static java.text.MessageFormat.format;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

public class NotesSteps extends AbstractIT {
    private static final String LISTING_COMMAND_CREATE_LISTING_NOTE = "listing.command.create-listing-note";
    private static final String MEDIA_TYPE = "application/vnd.listing.command.create-listing-note+json";
    private static final String LISTING_COMMAND_DELETE_LISTING_NOTE = "listing.command.delete-listing-note";
    private static final String DELETE_NOTE_MEDIA_TYPE = "application/vnd.listing.command.delete-listing-note+json";

    public Response createNoteForListing(final UUID courtRoomId, final String date, final String noteDescription) {
        final String createNoteForListingUrl = String.format("%s/%s", getBaseUri(),
                readConfig().getProperty(LISTING_COMMAND_CREATE_LISTING_NOTE));
        final RequestParams requestParams = requestParams(createNoteForListingUrl, MEDIA_TYPE)
                .withHeader(USER_ID, USER_ID_VALUE)
                .build();
        final JsonObject payload = Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("hearingDate", date)
                .add("noteDescription", noteDescription)
                .build();
        return restClient
                .postCommand(requestParams.getUrl(), requestParams.getMediaType(), payload.toString(), requestParams.getHeaders());
    }

    public Response editNoteForListing(final UUID noteId, final String noteDescription) {

        final String editNoteUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.command.edit-listing-note"),
                        noteId
                ));
        final RequestParams requestParams = requestParams(editNoteUrl, "application/vnd.listing.command.edit-listing-note+json")
                .withHeader(USER_ID, USER_ID_VALUE)
                .build();
        final JsonObject payload = Json.createObjectBuilder()
                .add("noteDescription", noteDescription)
                .build();
        return restClient.postCommand(requestParams.getUrl(), requestParams.getMediaType(), payload.toString(), requestParams.getHeaders());
    }

    public Response deleteNoteForListing(final UUID id) {
        final String deleteNoteForListingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.command.edit-listing-note"), id));
        final RequestParams requestParams = requestParams(deleteNoteForListingUrl, DELETE_NOTE_MEDIA_TYPE)
                .withHeader(USER_ID, USER_ID_VALUE)
                .build();
        final JsonObject payload = Json.createObjectBuilder()
                .build();
        return restClient
                .postCommand(requestParams.getUrl(), requestParams.getMediaType(), payload.toString(), requestParams.getHeaders());
    }
}

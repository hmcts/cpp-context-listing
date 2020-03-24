package uk.gov.moj.cpp.listing.command.utils.json;

import uk.gov.justice.listing.commands.PublishCourtList;
import uk.gov.moj.cpp.listing.command.fields.PublishCourtListFields;

import java.time.format.DateTimeFormatter;

import javax.json.Json;
import javax.json.JsonValue;

public class PublishCourtListJsonSupport {

    private static final DateTimeFormatter DATE_FORMAT_ZONED_DATE_AND_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter DATE_FORMAT_DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PublishCourtListJsonSupport() {
    }

    public static JsonValue asJson(final PublishCourtList publishCourtList) {
        final String requestedTimeAsOptionalString = publishCourtList.getRequestedTime()
                .map(x -> x.format(DATE_FORMAT_ZONED_DATE_AND_TIME)).orElse(null);
        return Json.createObjectBuilder()
                .add(PublishCourtListFields.COURT_CENTRE_ID.getInternalName(), publishCourtList.getCourtCentreId().toString())
                .add(PublishCourtListFields.START_DATE.getInternalName(), publishCourtList.getStartDate().format(DATE_FORMAT_DATE_ONLY))
                .add(PublishCourtListFields.END_DATE.getInternalName(), publishCourtList.getEndDate().format(DATE_FORMAT_DATE_ONLY))
                .add(PublishCourtListFields.TYPE.getInternalName(), publishCourtList.getPublishCourtListType().name())
                .add(PublishCourtListFields.REQUESTED_TIME.getInternalName(), requestedTimeAsOptionalString)
                .build();
    }

}

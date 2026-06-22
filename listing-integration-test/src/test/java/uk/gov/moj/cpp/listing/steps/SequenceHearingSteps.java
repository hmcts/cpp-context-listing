package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.LocalDate.parse;
import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceHearingSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceHearingSteps.class);

    private static final String EVENT_SELECTED_PUBLIC_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_SEQUENCED = "public.listing.hearing-days-sequenced";

    private static final String LISTING_COMMAND_SEQUENCE_HEARING_DAYS = "listing.command.sequence-hearings";
    private static final String MEDIA_TYPE_SEQUENCE_HEARING_DAYS = "application/vnd.listing.command.sequence-hearings+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private JmsMessageConsumerClient publicMessageConsumerHearingUpdated;

    private JmsMessageConsumerClient publicMessageConsumerHearingSequenced;

    private SequenceHearingData sequenceHearingData;

    private String request;

    public SequenceHearingSteps() {
    }

    public SequenceHearingSteps(SequenceHearingData sequenceHearingData) {
        this.sequenceHearingData = sequenceHearingData;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        createMessageConsumers();
    }

    public void whenHearingDaysAreSequenced() {
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_SEQUENCE_HEARING_DAYS), sequenceHearingData.getHearingId()));

        request = prepareJsonForSequenceHearingDays();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS, request);

        restClient.postCommand(updateHearingUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS, request, getLoggedInHeader());
    }

    public void verifyHearingDaysAreSequencedFromAPI() {
        final Filter idFilter = filter(where("id").is(sequenceHearingData.getHearingId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(sequenceHearingData.getUpdatedHearingData().getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(sequenceHearingData.getHearingId().toString())),
                withJsonPath("$.hearings[0].hearingDays[0].hearingDate",
                        equalTo(sequenceHearingData.getUpdatedHearingData().getStartDate())),
                withJsonPath("$.hearings[0].hearingDays[0].sequence",
                        equalTo(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getStartDate())))),
                withJsonPath("$.hearings[0].hearingDays[1].hearingDate",
                        equalTo(sequenceHearingData.getUpdatedHearingData().getEndDate())),
                withJsonPath("$.hearings[0].hearingDays[1].sequence",
                        equalTo(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getEndDate()))))
        });
    }

    public void verifyPublicEventHearingUpdated() {
        JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingUpdated);
        String startDate = sequenceHearingData.getUpdatedHearingData().getStartDate();
        String endDate = sequenceHearingData.getUpdatedHearingData().getEndDate();
        Integer sequence = sequenceHearingData.getSequencedDays().get(parse(startDate));
        Integer sequence1 = sequenceHearingData.getSequencedDays().get(parse(endDate));
        String startDateTime = sequenceHearingData.getUpdatedHearingData().getNonDefaultDays().get(0).getStartTime();
        ZonedDateTime endDateTime = ZonedDateTime.of(
                parse(sequenceHearingData.getUpdatedHearingData().getEndDate()), DEFAULT_START_TIME, UTC);

        assertThat(jsonResponse.get("updatedHearing.id"), is(sequenceHearingData.getHearingId().toString()));

        assertThat(jsonResponse.get("updatedHearing.hearingDays.size()"), is(2));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[0].listingSequence"), is(sequence));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[0].sittingDay"), is(fromString(startDateTime).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[1].listingSequence"), is(sequence1));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[1].sittingDay"), is(endDateTime.format(ZONED_DATE_TIME_FORMAT)));
    }


    private void createMessageConsumers() {
        publicMessageConsumerHearingUpdated = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_UPDATED);
        publicMessageConsumerHearingSequenced = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_SEQUENCED);
    }

    private String prepareJsonForSequenceHearingDays() {
        UUID hearingId = sequenceHearingData.getHearingId();
        Map<LocalDate, Integer> sequencedDays = sequenceHearingData.getSequencedDays();

        final JsonObjectBuilder builder = createObjectBuilder();
        JsonArrayBuilder hearingDays = JsonObjects.createArrayBuilder();

        for (Map.Entry<LocalDate, Integer> entry : sequencedDays.entrySet()) {
            hearingDays.add(createObjectBuilder()
                    .add("hearingDate", entry.getKey().toString())
                    .add("sequence", entry.getValue())
            );
        }
        builder.add("hearings", JsonObjects.createArrayBuilder().add(
                createObjectBuilder()
                        .add("id", hearingId.toString())
                        .add("sequenceHearingDays", hearingDays)));

        return builder.build().toString();
    }

    public void sequenceHearing(final JsonObject sequencedHearingJsonObject, final UUID hearingId) {
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_SEQUENCE_HEARING_DAYS), hearingId));

        request = sequencedHearingJsonObject.toString();

        restClient.postCommand(updateHearingUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS,
                request, getLoggedInHeader());
    }

    public void verifyHearingDaysAreSequencedForHearing(final UUID courtCentreId, final UUID courtRoomId, final String searchDate, final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"), ALLOCATED, courtCentreId, courtRoomId, searchDate));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

    public void verifyHearingDaySequencedPublicEvent() {
        JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingSequenced);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysSequenced: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(sequenceHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingDays.size()"), is(2));
        assertThat(jsonResponse.get("hearingDays[0].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getStartDate()));
        assertThat(jsonResponse.get("hearingDays[0].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getStartDate()))));
        assertThat(jsonResponse.get("hearingDays[1].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getEndDate()));
        assertThat(jsonResponse.get("hearingDays[1].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getEndDate()))));
    }
}



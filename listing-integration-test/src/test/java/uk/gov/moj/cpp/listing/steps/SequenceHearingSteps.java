package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.LocalDate.parse;
import static java.time.ZoneOffset.UTC;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceHearingSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceHearingSteps.class);

    private static final String EVENT_SELECTOR_HEARING_DAYS_SEQUENCED = "listing.events.hearing-days-sequenced";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";

    private static final String LISTING_COMMAND_SEQUENCE_HEARING_DAYS = "listing.command.sequence-hearings";
    private static final String MEDIA_TYPE_SEQUENCE_HEARING_DAYS = "application/vnd.listing.command.sequence-hearings+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private MessageConsumer privateMessageConsumerHearingDaysSequenced;
    private MessageConsumer privateMessageConsumerAllocatedHearingUpdatedForListing;
    private MessageConsumer publicMessageConsumerHearingUpdated;


    private final SequenceHearingData sequenceHearingData;

    private String request;


    public SequenceHearingSteps(SequenceHearingData sequenceHearingData) {
        this.sequenceHearingData = sequenceHearingData;
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        createMessageConsumers();

    }


    public void whenHearingDaysAreSequenced() {
        final String updateHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_SEQUENCE_HEARING_DAYS), sequenceHearingData.getHearingId()));

        request = prepareJsonForSequenceHearingDays();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS, request, getLoggedInHeader());

        restClient.postCommand(updateHearingUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS,
                request, getLoggedInHeader());

    }


    public void verifyHearingWithSequencedDaysInMQ() {
        verifyHearingDaySequences();
        verifyAllocatedHearingUpdatedForListing();
    }


    public void verifyHearingDaysAreSequencedFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), sequenceHearingData.getUpdatedHearingData().getCourtCentreId(), ALLOCATED));


        final Filter idFilter = filter(where("id").is(sequenceHearingData.getHearingId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
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
                                        equalTo(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getEndDate())))

                                ))));
    }


    public void verifyHearingUpdatedInPublicMQ() {

        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse);
    }

    @Override
    public void close() {
        try {

            closeMessageConsumers();

        } catch (JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }

    private void verifyAllocatedHearingUpdatedForListing() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerAllocatedHearingUpdatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerAllocatedHearingUpdatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(sequenceHearingData.getUpdatedHearingData().getHearingId().toString()));
        assertThat(jsonResponse.get("jurisdictionType"), is(sequenceHearingData.getUpdatedHearingData().getJurisdictionType()));
        assertThat(jsonResponse.get("courtRoomId"), is(sequenceHearingData.getUpdatedHearingData().getCourtRoomId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(sequenceHearingData.getUpdatedHearingData().getCourtCentreId().toString()));
        assertThat(jsonResponse.get("type.description"), is(sequenceHearingData.getUpdatedHearingData().getHearingTypData().getTypeDescription()));

        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(sequenceHearingData.getUpdatedHearingData().getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(sequenceHearingData.getUpdatedHearingData().getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get("judiciary[0].isDeputy"), is(sequenceHearingData.getUpdatedHearingData().getJudiciary().get(0).getIsDeputy().get()));
        assertThat(jsonResponse.get("judiciary[0].isBenchChairman"), is(sequenceHearingData.getUpdatedHearingData().getJudiciary().get(0).getIsBenchChairman().get()));

        assertThat(jsonResponse.get("hearingDays.size"), is(2));
        assertThat(jsonResponse.get("hearingDays[0].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getStartDate()));
        assertThat(jsonResponse.get("hearingDays[0].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getStartDate()))));
        assertThat(jsonResponse.get("hearingDays[1].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getEndDate()));
        assertThat(jsonResponse.get("hearingDays[1].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getEndDate()))));
    }


    private void createMessageConsumers() {
        privateMessageConsumerHearingDaysSequenced = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_SEQUENCED);
        publicMessageConsumerHearingUpdated = publicEvents.createConsumer(EVENT_SELECTED_PUBLIC_HEARING_UPDATED);
        privateMessageConsumerAllocatedHearingUpdatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING);


    }

    private void closeMessageConsumers() throws JMSException {
        privateMessageConsumerHearingDaysSequenced.close();
        publicMessageConsumerHearingUpdated.close();
        privateMessageConsumerAllocatedHearingUpdatedForListing.close();
    }




    private String prepareJsonForSequenceHearingDays() {
        UUID hearingId = sequenceHearingData.getHearingId();
        Map<LocalDate, Integer> sequencedDays = sequenceHearingData.getSequencedDays();

        final JsonObjectBuilder builder = createObjectBuilder();
        JsonArrayBuilder hearingDays = Json.createArrayBuilder();

        for (Map.Entry<LocalDate, Integer> entry : sequencedDays.entrySet()) {
            hearingDays.add(createObjectBuilder()
                    .add("hearingDate", entry.getKey().toString())
                    .add("sequence", entry.getValue())
            );
        }
        builder.add("hearings", Json.createArrayBuilder().add(
                createObjectBuilder()
                        .add("id", hearingId.toString())
                        .add("sequenceHearingDays", hearingDays)));

        return builder.build().toString();
    }


    private void verifyHearingPublicDetails(JsonPath jsonResponse) {
        String startDate = sequenceHearingData.getUpdatedHearingData().getStartDate();
        String endDate = sequenceHearingData.getUpdatedHearingData().getEndDate();
        Integer sequence = sequenceHearingData.getSequencedDays().get(parse(startDate));
        Integer sequence1 = sequenceHearingData.getSequencedDays().get(parse(endDate));
        String startDateTime = sequenceHearingData.getUpdatedHearingData().getNonDefaultDays().get(0).getStartTime();
        ZonedDateTime endDateTime = ZonedDateTime.of(
                parse(sequenceHearingData.getUpdatedHearingData().getEndDate()), DEFAULT_START_TIME, UTC) ;

        assertThat(jsonResponse.get("updatedHearing.id"), is(sequenceHearingData.getHearingId().toString()));

        assertThat(jsonResponse.get("updatedHearing.hearingDays.size"), is(2));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[0].listingSequence"), is(sequence));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[0].sittingDay"), is(fromString(startDateTime).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[1].listingSequence"), is(sequence1));
        assertThat(jsonResponse.get("updatedHearing.hearingDays[1].sittingDay"), is(endDateTime.format(ZONED_DATE_TIME_FORMAT)));


    }

    private void verifyHearingDaySequences() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysSequenced);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysSequenced: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(sequenceHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingDays.size"), is(2));
        assertThat(jsonResponse.get("hearingDays[0].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getStartDate()));
        assertThat(jsonResponse.get("hearingDays[0].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getStartDate()))));
        assertThat(jsonResponse.get("hearingDays[1].hearingDate"), is(sequenceHearingData.getUpdatedHearingData().getEndDate()));
        assertThat(jsonResponse.get("hearingDays[1].sequence"), is(sequenceHearingData.getSequencedDays().get(parse(sequenceHearingData.getUpdatedHearingData().getEndDate()))));
    }
}



package uk.gov.moj.cpp.listing.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreateWithRequestBody;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingDay;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.jayway.jsonpath.Filter;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelHearingSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelHearingSteps.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final String PUBLIC_HEARING_DAYS_CANCELLED = "public.hearing.hearing-days-cancelled";
    private static final String LISTING_COMMAND_SEQUENCE_HEARING_DAYS = "listing.command.sequence-hearings";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST_JSON = "application/vnd.listing.search.court.list+json";
    private static final String MEDIA_TYPE_SEQUENCE_HEARING_DAYS = "application/vnd.listing.command.sequence-hearings+json";

    private final HearingData hearingData;
    private final String hearingId;
    private final List<HearingDay> hearingDays;
    private final List<HearingDay> nonCancelledHearingDays;
    private final List<HearingDay> cancelledHearingDays;
    private final JmsMessageProducerClient publicEventHearingDaysCancelled;
    private JsonArray nonCancelledHearingDaysWithNewSequence;

    public CancelHearingSteps(final HearingsData hearingsData, final List<HearingDay> hearingDays) {
        this.hearingData = hearingsData.getHearingData().get(0);
        this.hearingId = hearingData.getId().toString();
        this.hearingDays = hearingDays;
        this.nonCancelledHearingDays = hearingDays.stream().filter(hearingDay -> !hearingDay.getIsCancelled().orElse(false)).collect(toList());
        this.cancelledHearingDays = hearingDays.stream().filter(hearingDay -> hearingDay.getIsCancelled().orElse(false)).collect(toList());
        this.publicEventHearingDaysCancelled = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    }

    public void whenPublicEventHearingDaysCancelledIsPublished() {
        final JsonObject jsonObject = getPayloadJsonObjectPreparedWithCancelledDays("public.hearing.hearing-days-cancelled.json");
        sendMessage(publicEventHearingDaysCancelled,
                PUBLIC_HEARING_DAYS_CANCELLED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_DAYS_CANCELLED)
                        .withUserId(USER_ID_VALUE.toString())
                        .build());
    }

    public void verifyHearingSlotsUpdatedToRetainNonCancelledDays() {
        verifyAzureUpdateApiInvoked();
    }

    public void verifyAzureUpdateApiInvoked() {
//TODO: expected PUT calls to courtscheduler are "list/hearingslots" with all three days followed by "/hearingslots" with two noncancelled days
// But this version is not making the second PUT to courtscheduler
        final RequestPatternBuilder nonCancelledDaysBuilder = putRequestedFor(urlEqualTo("/listingcourtscheduler-api/rest/courtscheduler/hearingslots"));
        nonCancelledHearingDays.forEach(day -> nonCancelledDaysBuilder.withRequestBody(containing("\"sessionDate\":\"" + day.getSittingDay().toLocalDate().toString() + "\"")));

        verify(2, nonCancelledDaysBuilder); // non cancelled days must be within azure api request body twice.

        final RequestPatternBuilder cancelledDaysBuilder = putRequestedFor(urlEqualTo("/listingcourtscheduler-api/rest/courtscheduler/hearingslots"));
        cancelledHearingDays.forEach(day -> cancelledDaysBuilder.withRequestBody(containing("\"sessionDate\":\"" + day.getSittingDay().toLocalDate().toString() + "\"")));

        verify(1, cancelledDaysBuilder); // cancelled days must be within azure api request body once.
    }

    public void verifyAllocatedHearingFoundOnNonCancelledHearingDay(final LocalDate searchDate) {
        final String allocatedHearingListUrl = getAllocatedHearingListUrl(searchDate);
        verifyHearingFound(allocatedHearingListUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON);
    }

    public void verifyAllocatedHearingNotFoundOnCancelledHearingDay(final LocalDate searchDate) {
        final String allocatedHearingListUrl = getAllocatedHearingListUrl(searchDate);
        verifyHearingNotFound(allocatedHearingListUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON);
    }

    public void verifyCourtListHearingFoundWithoutCancelledHearingDay(final CourtListType courtListType, final LocalDate startDate, final LocalDate endDate) {
        stubDocumentCreateWithRequestBody("testData", "");
        final String courtListUrl = getCourtListUrl(courtListType, startDate, endDate);
        verifyResponseReceived(courtListUrl, MEDIA_TYPE_SEARCH_COURT_LIST_JSON, OK);
    }

    public void verifyCourtListHearingFoundWithCancelledFalseHearingDay(final CourtListType courtListType, final LocalDate startDate, final LocalDate endDate) {
        stubDocumentCreateWithRequestBody("testData", "\"isCancelled\":false");
        final String courtListUrl = getCourtListUrl(courtListType, startDate, endDate);
        verifyResponseReceived(courtListUrl, MEDIA_TYPE_SEARCH_COURT_LIST_JSON, OK);
    }

    public void verifyCourtListHearingNotFound(final CourtListType courtListType, final LocalDate startDate, final LocalDate endDate) {
        stubDocumentCreate("testData", BAD_REQUEST);
        final String courtListUrl = getCourtListUrl(courtListType, startDate, endDate);
        verifyResponseReceived(courtListUrl, MEDIA_TYPE_SEARCH_COURT_LIST_JSON, BAD_REQUEST);
    }

    public void verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtLists() {
        pollForHearingWithJmsDelay(this.hearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), getMatchersForHearingFoundWithCancelledDaysRemoved(false));
    }

    public void verifyAllocatedHearingFoundWhenSearchDateWithinStartAndEndDateRangeForCourtLists(final LocalDate localDate) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED, this.hearingData.getCourtCentreId(), to(localDate), to(localDate)));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), getMatchersForHearingFoundWithCancelledDaysRemoved(false));
    }

    public void verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtListsOnWeekCommencingRange() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.by.week.commencing"),
                        to(hearingData.getHearingStartDate()), to(hearingData.getHearingStartDate().plusDays(7)), hearingData.getCourtCentreId(), ALLOCATED));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), getMatchersForHearingFoundWithCancelledDaysRemoved(false));
    }

    public void verifyAllocatedHearingFoundWhenSearchDateWithinWeekCommencingRangeForCourtLists(final LocalDate localDate) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.by.week.commencing"),
                        to(localDate.minusDays(3)), to(localDate.plusDays(3)), hearingData.getCourtCentreId(), ALLOCATED));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), getMatchersForHearingFoundWithCancelledDaysRemoved(false));
    }

    public void whenHearingDaysAreSequenced() {
        final String sequenceHearingsUrl = String.format("%s/%s", getBaseUri(), readConfig().getProperty(LISTING_COMMAND_SEQUENCE_HEARING_DAYS));

        final String request = prepareJsonForSequenceHearingDays();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", sequenceHearingsUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS, request);

        restClient.postCommand(sequenceHearingsUrl, MEDIA_TYPE_SEQUENCE_HEARING_DAYS, request, getLoggedInHeader());
    }

    public void verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtListsWithUpdatedSequence() {
        pollForHearing(this.hearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), getMatchersForHearingFoundWithCancelledDaysRemoved(true));
    }

    private Matcher[] getMatchersForHearingFoundWithCancelledDaysRemoved(final boolean sequenceUpdated) {
        final List<Matcher> matchersOnPayload = new ArrayList<>();
        matchersOnPayload.add(withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())));
        matchersOnPayload.add(withJsonPath("$.hearings[0].hearingDays", hasSize(nonCancelledHearingDays.size())));

        for (int index = 0; index < nonCancelledHearingDays.size(); index++) {
            matchersOnPayload.add(withJsonPath(String.format("$.hearings[0].hearingDays[%s].hearingDate", index), equalTo(to(hearingDays.get(index).getSittingDay().toLocalDate()))));
            matchersOnPayload.add(withJsonPath(String.format("$.hearings[0].hearingDays[%s].isCancelled", index), equalTo(false)));
            if (sequenceUpdated) {
                final int sequence = nonCancelledHearingDaysWithNewSequence.getJsonObject(index).getInt("sequence");
                matchersOnPayload.add(withJsonPath(String.format("$.hearings[0].hearingDays[%s].sequence", index), equalTo(sequence)));
            }
        }

        return matchersOnPayload.toArray(new Matcher[0]);
    }

    private String getAllocatedHearingListUrl(final LocalDate searchDate) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                        ALLOCATED, hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), to(searchDate)));
    }

    private String getCourtListUrl(final CourtListType courtListType, final LocalDate startDate, final LocalDate endDate) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.court.list"),
                        hearingData.getCourtCentreId(), startDate, courtListType.toString(), endDate));
    }

    private void verifyHearingFound(final String searchHearingsUrl, final String mediaType) {
        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = compile("$.hearings[?]", idFilter);
        pollWithDefaults(requestParams(searchHearingsUrl, mediaType).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath(hearingIdFilter)
                        ));
    }

    private void verifyHearingNotFound(final String searchHearingsUrl, final String mediaType) {
        poll(requestParams(searchHearingsUrl, mediaType).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearings", emptyCollection())
                        ));
    }

    private void verifyResponseReceived(final String searchHearingsUrl, final String mediaType, final Status expectedStatus) {
        poll(requestParams(searchHearingsUrl, mediaType).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(expectedStatus));
    }

    private String prepareJsonForSequenceHearingDays() {
        final JsonArray hearingDays = nonCancelledHearingDays.stream()
                .map(hearingDay -> createObjectBuilder()
                        .add("hearingDate", to(hearingDay.getSittingDay().toLocalDate()))
                        .add("sequence", integer(100).next())
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        this.nonCancelledHearingDaysWithNewSequence = hearingDays;

        return createObjectBuilder()
                .add("hearings", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", hearingId)
                                .add("sequenceHearingDays", hearingDays)))
                .build()
                .toString();
    }

    private JsonObject getPayloadJsonObjectPreparedWithCancelledDays(final String path) {
        final String eventPayloadString = getPayloadStringPreparedWithCancelledDays(path);
        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    private String getPayloadStringPreparedWithCancelledDays(final String path) {
        String eventPayloadString = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DAY_1_SITTING_DAY", hearingDays.get(0).getSittingDay().format(DATE_TIME_FORMATTER))
                .replaceAll("DAY_2_SITTING_DAY", hearingDays.get(1).getSittingDay().format(DATE_TIME_FORMATTER))
                .replaceAll("DAY_3_SITTING_DAY", hearingDays.get(2).getSittingDay().format(DATE_TIME_FORMATTER));

        if (hearingDays.get(0).getIsCancelled().isPresent()) {
            eventPayloadString = eventPayloadString.replaceAll("DAY_1_IS_CANCELLED", Boolean.toString(hearingDays.get(0).getIsCancelled().get()));
        } else {
            eventPayloadString = eventPayloadString.replaceAll(",\n\\s+\"isCancelled\": DAY_1_IS_CANCELLED", "");
        }

        if (hearingDays.get(1).getIsCancelled().isPresent()) {
            eventPayloadString = eventPayloadString.replaceAll("DAY_2_IS_CANCELLED", Boolean.toString(hearingDays.get(1).getIsCancelled().get()));
        } else {
            eventPayloadString = eventPayloadString.replaceAll(",\n\\s+\"isCancelled\": DAY_2_IS_CANCELLED", "");
        }

        if (hearingDays.get(2).getIsCancelled().isPresent()) {
            eventPayloadString = eventPayloadString.replaceAll("DAY_3_IS_CANCELLED", Boolean.toString(hearingDays.get(2).getIsCancelled().get()));
        } else {
            eventPayloadString = eventPayloadString.replaceAll(",\n\\s+\"isCancelled\": DAY_3_IS_CANCELLED", "");
        }

        return eventPayloadString;
    }
}

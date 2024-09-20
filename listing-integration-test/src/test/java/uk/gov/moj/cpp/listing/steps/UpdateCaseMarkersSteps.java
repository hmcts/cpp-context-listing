package uk.gov.moj.cpp.listing.steps;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCaseMarkersSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCaseMarkersSteps.class);
    private static final String PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";
    private static final String PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED = "listing.events.case-markers-to-be-updated";
    public static final String PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";
    JSONComparator ignoreMetaDataComparator = new CustomComparator(JSONCompareMode.LENIENT, new Customization("_metadata", (o1, o2) -> true));


    private JmsMessageProducerClient publicEventCaseMarkersToBeUpdated;
    private JmsMessageConsumerClient publicEventMessageConsumerCaseMarkersToBeUpdated;

    private JmsMessageConsumerClient privateEventMessageCaseMarkersToBeUpdated;
    private JmsMessageConsumerClient publicMessageConsumerHmiHearingUpdated;

    private String request;

    private final HearingData hearingData;
    private final CaseMarkerData caseMarkerData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;
    private final UUID metadataId;
    private final UUID userId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public UpdateCaseMarkersSteps(final UUID caseId, final HearingData hearingData, final CaseMarkerData caseMarkerData) {
        this.hearingData = hearingData;
        this.caseMarkerData = caseMarkerData;
        this.listedCaseData = hearingData.getListedCases().get(0);
        this.caseId = caseId;
        metadataId = randomUUID();
        userId = randomUUID();

        publicEventCaseMarkersToBeUpdated = publicEvents.createPublicProducer();
        publicEventMessageConsumerCaseMarkersToBeUpdated = publicEvents.createPublicConsumer(PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED);
        privateEventMessageCaseMarkersToBeUpdated = privateEvents.createPrivateConsumer(PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED);
        publicMessageConsumerHmiHearingUpdated = publicEvents.createPublicConsumer(PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseMarkerUpdatedPublicEventIsPublished() {
        final CaseMarkersUpdated caseMarkersUpdated = getCaseMarkerUpdate(caseId, hearingData.getId());
        publishCaseMarkersUpdated(caseMarkersUpdated);
    }


    public CaseMarkersUpdated getCaseMarkerUpdate(final UUID caseId, final UUID hearingId) {
        return CaseMarkersUpdated.caseMarkersUpdated()
                .withCaseMarkers(buildCaseMarkers())
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId)
                .build();
    }

    private List<Marker> buildCaseMarkers() {
        return Collections.singletonList(Marker.marker()
                .withId(caseMarkerData.getId())
                .withMarkerTypeid(caseMarkerData.getCaseMarkerTypeId())
                .withMarkerTypeCode(caseMarkerData.getCaseMarkerCode())
                .withMarkerTypeDescription(caseMarkerData.getCaseMarkerDescription())
                .build());
    }

    private void publishCaseMarkersUpdated(final CaseMarkersUpdated caseMarkersUpdated) {
        final JsonObject updateCaseMarkersObject = (JsonObject) objectToJsonValueConverter.convert(caseMarkersUpdated);

        QueueUtil.sendMessage(
                publicEventCaseMarkersToBeUpdated,
                PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED,
                updateCaseMarkersObject,
                metadataOf(metadataId, PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED).withUserId(userId.toString()).build());

        request = updateCaseMarkersObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED, request, getLoggedInHeader());
    }

    public void verifyPublicEventCaseMarkersUpdatedInActiveMQ() throws Exception {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final String jsonResponse = QueueUtil.retrieveMessageString(publicEventMessageConsumerCaseMarkersToBeUpdated);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerDefendantOffencesUpdated: {}", jsonResponse);

        assertThat(jsRequest.getString("hearingId"), is(hearingData.getId().toString()));
        assertThat(jsRequest.getString("prosecutionCaseId"), is(caseId.toString()));
        assertThat(jsRequest.getList("caseMarkers").size(), is(1));
        assertThat(jsRequest.get("caseMarkers[0].id"), is(caseMarkerData.getId().toString()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeid"), is(caseMarkerData.getCaseMarkerTypeId().toString()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeDescription"), is(caseMarkerData.getCaseMarkerDescription()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeCode"), is(caseMarkerData.getCaseMarkerCode()));

    }

    public void verifyEventCaseMarkersToBeUpdateInActiveMQ() throws Exception {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final String jsonResponse = QueueUtil.retrieveMessageString(privateEventMessageCaseMarkersToBeUpdated);
        LOGGER.debug("jsonResponse from privateEventMessageOffencesToBeUpdated: {}", jsonResponse);

        assertThat(jsRequest.getString("hearingId"), is(hearingData.getId().toString()));
        assertThat(jsRequest.getString("prosecutionCaseId"), is(caseId.toString()));
        assertThat(jsRequest.getList("caseMarkers").size(), is(1));
        assertThat(jsRequest.get("caseMarkers[0].id"), is(caseMarkerData.getId().toString()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeid"), is(caseMarkerData.getCaseMarkerTypeId().toString()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeDescription"), is(caseMarkerData.getCaseMarkerDescription()));
        assertThat(jsRequest.get("caseMarkers[0].markerTypeCode"), is(caseMarkerData.getCaseMarkerCode()));
    }

    public void verifyHmiPublicEventForUpdateHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHmiHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHmiHearingUpdated: {}", jsonResponse.prettify());
        Assert.assertThat(jsonResponse.get("hearing.id"), CoreMatchers.is(hearingData.getId().toString()));
    }
}

package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;

public class UpdateCaseMarkersSteps extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";

    private JmsMessageProducerClient publicEventCaseMarkersToBeUpdated;

    private final HearingData hearingData;
    private final CaseMarkerData caseMarkerData;
    private final UUID caseId;
    private final UUID metadataId;
    private final UUID userId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public UpdateCaseMarkersSteps(final UUID caseId, final HearingData hearingData, final CaseMarkerData caseMarkerData) {
        this.hearingData = hearingData;
        this.caseMarkerData = caseMarkerData;
        this.caseId = caseId;
        metadataId = randomUUID();
        userId = randomUUID();

        publicEventCaseMarkersToBeUpdated = publicEvents.createPublicProducer();

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

        sendMessage(
                publicEventCaseMarkersToBeUpdated,
                PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED,
                updateCaseMarkersObject,
                metadataOf(metadataId, PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED).withUserId(userId.toString()).build());
    }

    public void verifyCaseMarkersUpdatedThroughAPI(final UUID caseIdToUpdateMarkers) {

        pollForHearing(hearingData.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].id", hasItem(caseMarkerData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeid", hasItem(caseMarkerData.getCaseMarkerTypeId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeDescription", hasItem(caseMarkerData.getCaseMarkerDescription())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeCode", hasItem(caseMarkerData.getCaseMarkerCode()))
        });
    }

}

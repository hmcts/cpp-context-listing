package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.it.util.PublishRetryHelper.publishUntilReflected;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCaseMarkersSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCaseMarkersSteps.class);

    private static final String PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";

    private JmsMessageProducerClient publicEventCaseMarkersToBeUpdated;

    private final HearingData hearingData;
    private final CaseMarkerData caseMarkerData;
    private final UUID caseId;
    private final UUID userId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public UpdateCaseMarkersSteps(final UUID caseId, final HearingData hearingData, final CaseMarkerData caseMarkerData) {
        this.hearingData = hearingData;
        this.caseMarkerData = caseMarkerData;
        this.caseId = caseId;
        userId = randomUUID();

        publicEventCaseMarkersToBeUpdated = publicEvents.createPublicProducer();

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseMarkerUpdatedPublicEventIsPublished() {
        final CaseMarkersUpdated caseMarkersUpdated = getCaseMarkerUpdate(caseId, hearingData.getId());
        publishCaseMarkersUpdated(caseMarkersUpdated);
    }

    /**
     * Publishes the {@code public.progression.case-markers-updated} event and verifies the read model,
     * RE-PUBLISHING until the update is reflected (or the attempt budget is exhausted).
     *
     * <p><b>Why re-publish instead of publish-once-then-poll?</b> The event is consumed by
     * {@code ListingEventProcessor} and routed to {@code listing.command.update-case-markers}, handled by
     * the {@code Case} aggregate's {@code addedCaseMarkers(...)}. That method <b>silently drops the update</b>
     * ({@code if (hearingIds.isEmpty()) return Stream.empty();}) when the aggregate does not yet know which
     * hearing the case belongs to. {@code Case.hearingIds} is populated only after the asynchronous
     * {@code add-hearing-to-case} command runs — itself triggered by a private event emitted <i>after</i>
     * {@code list-court-hearing} — and {@code HearingAddedToCase} has <b>no viewstore projection</b>, so the
     * test cannot deterministically await the link. On slower environments (the vld validation pipeline) the
     * first publish can be processed before the link exists; it is then dropped with no JMS redelivery, so a
     * single publish is lost forever and the 90s poll can never succeed. This was the root cause of
     * CaseMarkerUpdateIT failing on vld (builds 651954 / 653051) while passing locally — a cross-aggregate
     * eventual-consistency race against a deliberate, correct production guard (you cannot mark a case that is
     * not yet linked to a hearing). Re-publishing (with a fresh event id each time) guarantees that once the
     * link is established, a subsequent publish lands.
     */
    public void publishUntilCaseMarkersReflected(final UUID caseIdToUpdateMarkers) {
        publishUntilReflected(LOGGER, "case-marker-fix", "case-markers-updated for case " + caseIdToUpdateMarkers,
                this::whenCaseMarkerUpdatedPublicEventIsPublished,
                () -> verifyCaseMarkersUpdatedThroughAPIWithJmsDelay(caseIdToUpdateMarkers));
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

        // Fresh event id per publish: the framework dedupes events by metadata id, so re-publishing
        // with the same id would be ignored. A new id guarantees each re-publish is reprocessed.
        sendMessage(
                publicEventCaseMarkersToBeUpdated,
                PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED,
                updateCaseMarkersObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_CASE_PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED).withUserId(userId.toString()).build());
    }

    public void verifyCaseMarkersUpdatedThroughAPI(final UUID caseIdToUpdateMarkers) {

        pollForHearing(hearingData.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].id", hasItem(caseMarkerData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeid", hasItem(caseMarkerData.getCaseMarkerTypeId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeDescription", hasItem(caseMarkerData.getCaseMarkerDescription())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeCode", hasItem(caseMarkerData.getCaseMarkerCode()))
        });
    }

    /**
     * JMS-aware version of verifyCaseMarkersUpdatedThroughAPI for handling asynchronous message processing timing issues.
     */
    public void verifyCaseMarkersUpdatedThroughAPIWithJmsDelay(final UUID caseIdToUpdateMarkers) {

        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].id", hasItem(caseMarkerData.getId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeid", hasItem(caseMarkerData.getCaseMarkerTypeId().toString())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeDescription", hasItem(caseMarkerData.getCaseMarkerDescription())),
                withJsonPath("$.hearings[0].listedCases[?(@.id == '" + caseIdToUpdateMarkers + "')].markers[0].markerTypeCode", hasItem(caseMarkerData.getCaseMarkerCode()))
        });
    }

}

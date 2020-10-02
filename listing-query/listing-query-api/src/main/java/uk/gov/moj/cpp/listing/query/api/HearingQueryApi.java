package uk.gov.moj.cpp.listing.query.api;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataLoader;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_API)
public class HearingQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryApi.class);

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String OU_L2_CODE = "oucodeL2Code";


    @Inject
    private HearingQueryView hearingQueryView;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataLoader referenceDataLoader;

    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        return hearingQueryView.searchHearings(query);
    }

    @Handles("listing.unscheduled.search.hearings")
    public Envelope<JsonObject> searchUnscheduledHearings(final JsonEnvelope query) {
        final JsonObject jsonObject = query.payloadAsJsonObject();
        final String courtCentreId = jsonObject.getString(COURT_CENTRE_ID, null);
        final String oucodeL2Code = jsonObject.getString(OU_L2_CODE, null);
        final JsonObjectBuilder objectBuilder = createObjectBuilderWithFilter(jsonObject,
                keyName -> (!COURT_CENTRE_ID.equalsIgnoreCase(keyName)) && (!OU_L2_CODE.equalsIgnoreCase(keyName)));

        if (Strings.isNullOrEmpty(courtCentreId)) {
            if (!Strings.isNullOrEmpty(oucodeL2Code)) {
                final List<OrganisationUnit> organisationUnits = referenceDataLoader.fetchOrganisationUnitsByOucodeL2Code(oucodeL2Code);
                final String courtCentreIds = organisationUnits.stream().map(OrganisationUnit::getId).map(UUID::toString).collect(Collectors.joining(","));
                objectBuilder.add(COURT_CENTRE_IDS, courtCentreIds);
            }
        } else {
            objectBuilder.add(COURT_CENTRE_IDS, courtCentreId);
        }
        return hearingQueryView.searchUnscheduledHearings(envelopeFrom(query.metadata(), objectBuilder.build()));
    }

    @Handles("listing.available.search.hearings")
    public JsonEnvelope searchAvailableHearings(final JsonEnvelope query) throws IOException {
        return hearingQueryView.searchAvailableHearings(query);
    }

    @Handles("listing.allocated.and.unallocated.hearings")
    public JsonEnvelope searchUnallocatedHearings(final JsonEnvelope query) {
        return hearingQueryView.searchAllocatedAndUnallocatedHearings(query);
    }
    @Handles("listing.any-allocation.search.hearings")
    public Envelope<JsonObject> searchHearingsWithAnyAllocationState(final JsonEnvelope query)  {
        return hearingQueryView.searchHearingsWithAnyAllocationState(query);
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSsearchHearings(final JsonEnvelope query) {
        return hearingQueryView.rangeSearchHearings(query);
    }

    @Handles("listing.search.court.list")
    public JsonEnvelope searchHearingsForCourtList(final JsonEnvelope query) {
        return hearingQueryView.getCourtListContent(query);
    }

    @Handles("listing.search.hearing")
    public JsonEnvelope searchForHearingById(final JsonEnvelope query) {
        ensureThatHearingIdIsAValidUUID(query);
        return hearingQueryView.getHearingById(query);
    }

    private void ensureThatHearingIdIsAValidUUID(final JsonEnvelope query) {
        final String rawId = query.payloadAsJsonObject().getString("id", null);
        if (rawId == null) {
            final String message = "Attempted to search for a Hearing without an ID.";
            LOGGER.warn(message);
            throw new IllegalArgumentException(message);
        }
        try {
            UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Please ensure that the id is a valid UUID.", ex);
        }
    }

    @Handles("listing.court.list.publish.status")
    public JsonEnvelope publishCourtListStatus(final JsonEnvelope query) {
        return hearingQueryView.getCourtListPublishStatus(query);
    }
}

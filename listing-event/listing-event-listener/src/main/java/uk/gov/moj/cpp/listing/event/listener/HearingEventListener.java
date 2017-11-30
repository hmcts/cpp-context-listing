package uk.gov.moj.cpp.listing.event.listener;


import static java.lang.String.format;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.HearingUnallocatedForListing;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;
import uk.gov.moj.cpp.listing.event.converter.UnallocatedHearingListedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    private static final boolean ALLOCATED = true;
    private static final String URN = "urn";
    private static final String CASE_ID = "caseId";
    private static final boolean UNALLOCATED = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventListener.class);


    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Inject
    private UnallocatedHearingListedConverter unallocatedHearingListedConverter;


    @Handles("listing.events.case-sent-for-listing")
    public void caseSentForListing(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String urn = payload.getString(URN);
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final ListingCase listingCase = createListingCase(urn, caseId);
        if (!listingCaseExists(caseId)) {
            listingCaseRepository.save(listingCase);
        }
    }

    @Handles("listing.events.unallocated-hearing-listed")
    public void unallocatedHearingListed(final JsonEnvelope event) {
        final Hearing hearing = unallocatedHearingListedConverter.convert(jsonObjectConverter
                .convert(event.payloadAsJsonObject(), UnallocatedHearingListed.class));
        hearingRepository.save(hearing);

    }

    @Handles("listing.events.hearing-allocated-for-listing")
    public void hearingAllocatedForHearing(final JsonEnvelope event) {
        final HearingAllocatedForListing hearingAllocatedForListing = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class);
        final UUID hearingId = UUID.fromString(hearingAllocatedForListing.getHearingId());
        LOGGER.info(format("'listing.events.hearing-allocated-for-listing' received with hearingId %s", hearingId));
        hearingRepository.updateAllocated(ALLOCATED, hearingId);
    }


    @Handles("listing.events.hearing-unallocated-for-listing")
    public void hearingUnallocatedForHearing(final JsonEnvelope event) {
        final HearingUnallocatedForListing hearingUnallocatedForListing = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingUnallocatedForListing.class);
        final UUID hearingId = UUID.fromString(hearingUnallocatedForListing.getHearingId());
        LOGGER.info(format("'listing.events.hearing-unallocated-for-listing' received with hearingId %s", hearingId));
        hearingRepository.updateAllocated(UNALLOCATED, hearingId);
    }

    private boolean listingCaseExists(final UUID caseId) {
        return caseId != null && listingCaseRepository.findBy(caseId)!=null;
    }

    private ListingCase createListingCase(final String urn, final UUID caseId) {
        return new ListingCaseBuilder()
                .setUrn(urn)
                .setCaseId(caseId)
                .build();
    }
}

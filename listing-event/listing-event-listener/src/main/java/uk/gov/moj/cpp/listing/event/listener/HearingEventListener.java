package uk.gov.moj.cpp.listing.event.listener;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
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

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    private static final boolean ALLOCATED = true;
    public static final String URN = "urn";
    public static final String CASE_ID = "caseId";

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
        hearingRepository.updateAllocated(ALLOCATED, hearingId);
    }

    private boolean listingCaseExists(UUID caseId) {
        return caseId != null && listingCaseRepository.findBy(caseId)!=null;
    }

    private ListingCase createListingCase(String urn, UUID caseId) {
        return new ListingCaseBuilder()
                .setUrn(urn)
                .setCaseId(caseId)
                .build();
    }
}

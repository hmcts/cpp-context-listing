package uk.gov.moj.cpp.listing.event.listener;


import static java.lang.String.format;

import uk.gov.justice.listing.events.CaseSentForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.HearingListedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    private static final boolean ALLOCATED = true;
    private static final boolean UNALLOCATED = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventListener.class);


    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Inject
    private HearingListedConverter hearingListedConverter;


    @Handles("listing.events.case-sent-for-listing")
    public void caseSentForListing(final Envelope<CaseSentForListing> event) {
        UUID caseId = event.payload().getCaseId();
        final ListingCase listingCase = createListingCase(event.payload().getUrn(), caseId);
        if (!listingCaseExists(caseId)) {
            listingCaseRepository.save(listingCase);
        }
    }

    @Handles("listing.events.hearing-listed")
    public void hearingListed(final Envelope<HearingListed> event) {
        final Hearing hearing = hearingListedConverter.convert(event.payload());
        hearingRepository.save(hearing);

    }

    @Handles("listing.events.hearing-allocated-for-listing")
    public void hearingAllocatedForHearing(final Envelope<HearingAllocatedForListing> event) {
        final HearingAllocatedForListing hearingAllocatedForListing = event.payload();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();
        LOGGER.info(format("'listing.events.hearing-allocated-for-listing' received with hearingId %s", hearingId));
        hearingRepository.updateAllocated(ALLOCATED, hearingId);
    }


    @Handles("listing.events.hearing-unallocated-for-listing")
    public void hearingUnallocatedForHearing(final Envelope<HearingUnallocatedForListing> event) {
        final HearingUnallocatedForListing hearingUnallocatedForListing = event.payload();
        final UUID hearingId = hearingUnallocatedForListing.getHearingId();
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

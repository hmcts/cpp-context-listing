package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder;

import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    private static final boolean ALLOCATED = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventListener.class);
    private JsonEntityFinder jsonEntityFinder;
    private HearingRepository hearingRepository;
    private ObjectMapper mapper;

    @Inject
    public HearingEventListener(final HearingRepository hearingRepository,
                                final ObjectMapper mapper) {
        this.hearingRepository = hearingRepository;
        this.jsonEntityFinder = JsonEntityFinder.using(hearingRepository);
        this.mapper = mapper;
    }

    @Handles("listing.events.hearing-listed")
    public void hearingListed(final Envelope<HearingListed> event) {
        HearingListed hearingListed = event.payload();
        JsonNode hearingJsonNode = convertToJsonNode(hearingListed.getHearing());
        UUID hearingId = hearingListed.getHearing().getId();
        LOGGER.info("'listing.events.hearing-listed' received hearingId {}", hearingId);
        final Hearing hearing = new Hearing(hearingId, hearingJsonNode);
        hearingRepository.save(hearing);
    }

    @Handles("listing.events.hearing-allocated-for-listing")
    public void hearingAllocated(final Envelope<HearingAllocatedForListing> event) {
        final HearingAllocatedForListing hearingAllocatedForListing = event.payload();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();
        LOGGER.info("'listing.events.hearing-allocated-for-listing' received hearingId {}", hearingId);
        jsonEntityFinder.find(hearingId).put("allocated", ALLOCATED).save();
    }

    @Handles("listing.events.hearing-unallocated-for-listing")
    public void hearingUnallocated(final Envelope<HearingUnallocatedForListing> event) {
        final HearingUnallocatedForListing hearingUnallocatedForListing = event.payload();
        final UUID hearingId = hearingUnallocatedForListing.getHearingId();
        LOGGER.info("'listing.events.hearing-unallocated-for-listing' received hearingId {}", hearingId);
        jsonEntityFinder.find(hearingId).put("allocated", !ALLOCATED).save();

    }

    private JsonNode convertToJsonNode(Object source) {
        return mapper.valueToTree(source);
    }


}

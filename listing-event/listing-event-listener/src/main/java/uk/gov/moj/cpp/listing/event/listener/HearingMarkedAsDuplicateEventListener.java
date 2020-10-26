package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingMarkedAsDuplicateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateEventListener.class);


    private HearingRepository hearingRepository;

    @Inject
    public HearingMarkedAsDuplicateEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.hearing-marked-as-duplicate")
    public void deleteHearing(final Envelope<HearingMarkedAsDuplicate> event) {
        final UUID hearingId = event.payload().getHearingId();
        LOGGER.debug("listing.events.hearing-marked-as-duplicate received. hearingId: {} ", hearingId);

        final Hearing hearing = hearingRepository.findBy(hearingId);
        if (Objects.nonNull(hearing)) {
            hearingRepository.remove(hearing);
            LOGGER.debug("listing.events.hearing-marked-as-duplicate received. hearingId: {} exist. Hearing removed", hearingId);
        }

    }
}

package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.DefendantDetailsUpdated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.SimpleDefendantConverter;
import uk.gov.moj.cpp.listing.event.converter.StartTimesJsonConverter;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendant;
import uk.gov.moj.cpp.listing.persistence.repository.SimpleDefendantRepository;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class DefendantEventListener {

    @Inject
    private SimpleDefendantRepository simpleDefendantRepository;

    @Inject
    private StartTimesJsonConverter startTimesConverter;

    @Inject
    SimpleDefendantConverter simpleDefendantConverter;

    @Handles("listing.events.defendant-details-updated")
    public void defendantDetailsUpdated(final Envelope<DefendantDetailsUpdated> event) {
        final DefendantDetailsUpdated defendantDetailsUpdated = event.payload();
        final SimpleDefendant defendant = simpleDefendantConverter.convert(defendantDetailsUpdated);

        simpleDefendantRepository.save(defendant);
    }
}

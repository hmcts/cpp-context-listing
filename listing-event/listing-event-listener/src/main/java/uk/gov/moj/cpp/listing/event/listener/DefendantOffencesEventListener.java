package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.BaseOffenceConverter;
import uk.gov.moj.cpp.listing.event.converter.OffenceWithDefendantIdConverter;
import uk.gov.moj.cpp.listing.event.converter.StartTimesJsonConverter;
import uk.gov.moj.cpp.listing.persistence.entity.BaseOffence;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceId;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceIdBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceWithDefendantId;
import uk.gov.moj.cpp.listing.persistence.repository.BaseOffenceRepository;
import uk.gov.moj.cpp.listing.persistence.repository.OffenceWithDefendantIdRepository;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class DefendantOffencesEventListener {

    @Inject
    private BaseOffenceRepository offenceRepository;

    @Inject
    private OffenceWithDefendantIdRepository offenceWithDefendantIdRepository;

    @Inject
    private StartTimesJsonConverter startTimesConverter;

    @Inject
    BaseOffenceConverter baseOffenceConverter;

    @Inject
    OffenceWithDefendantIdConverter offenceWithDefendantIdConverter;

    @Handles("listing.events.offence-updated")
    public void offenceUpdated(final Envelope<OffenceUpdated> event) {
        final OffenceUpdated offenceUpdated = event.payload();
        final BaseOffence offence = baseOffenceConverter.convert(offenceUpdated);

        offenceRepository.save(offence);
    }

    @Handles("listing.events.offence-deleted")
    public void offenceDeleted(final Envelope<OffenceDeleted> event) {
        final OffenceDeleted offenceDeleted = event.payload();

        final CompositeOffenceId compositeOffenceId = new CompositeOffenceIdBuilder()
                .setHearingId(offenceDeleted.getHearingId())
                .setDefendantId(offenceDeleted.getDefendantId())
                .setOffenceId(offenceDeleted.getOffenceId())
                .build();

        offenceRepository.removeById(compositeOffenceId);
    }

    @Handles("listing.events.offence-added")
    public void offenceAdded(final Envelope<OffenceAdded> event) {
        final OffenceAdded offenceAdded = event.payload();
        final OffenceWithDefendantId offence = offenceWithDefendantIdConverter.convert(offenceAdded);

        offenceWithDefendantIdRepository.save(offence);
    }
}

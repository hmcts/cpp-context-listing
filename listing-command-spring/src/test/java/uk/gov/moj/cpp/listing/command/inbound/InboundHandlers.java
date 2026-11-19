package uk.gov.moj.cpp.listing.command.inbound;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventHandler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.List;

import jakarta.persistence.EntityManager;

/** Every inbound handler of the application, built as Spring builds them (test support). */
final class InboundHandlers {

    private InboundHandlers() {
    }

    static List<InboundEventHandler> all(final ProcessorQueue processorQueue, final ProcessorSender sender,
                                         final HearingSlotsService hearingSlotsService, final HearingQueryView hearingQueryView,
                                         final EntityManager entityManager) {
        return List.of(
                new ListingPublicEventProcessor(processorQueue, sender, hearingSlotsService),
                new CpsProsecutorUpdatedEventProcessor(processorQueue, sender, hearingQueryView, entityManager),
                new HearingResultedEventProcessor(processorQueue, sender),
                new CaseRemovedFromGroupCasesEventProcessor(processorQueue, sender),
                new CourtApplicationDeletedProcessor(processorQueue, sender),
                new HearingCounselEventProcessor(processorQueue, sender),
                new RelatedHearingUpdatedForAdhocHearingProcessor(processorQueue, sender));
    }
}

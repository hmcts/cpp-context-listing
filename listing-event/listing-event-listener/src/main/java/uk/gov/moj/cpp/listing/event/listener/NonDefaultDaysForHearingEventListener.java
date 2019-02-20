package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class NonDefaultDaysForHearingEventListener {

    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.non-default-days-assigned-to-hearing")
    public void nonDefaultDaysAssignedForHearing(final Envelope<NonDefaultDaysAssignedToHearing> event) {
        final NonDefaultDaysAssignedToHearing nonDefaultDaysAssignedToHearing = event.payload();
        final List<NonDefaultDay> nonDefaultDays = nonDefaultDaysAssignedToHearing.getNonDefaultDays();
        final UUID hearingId = nonDefaultDaysAssignedToHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObjectList(NON_DEFAULT_DAYS, nonDefaultDays)
                .save();
    }

    @Handles("listing.events.non-default-days-changed-for-hearing")
    public void nonDefaultDaysChangedForHearing(final Envelope<NonDefaultDaysChangedForHearing> event) {
        final NonDefaultDaysChangedForHearing nonDefaultDaysChangedForHearing =  event.payload();
        final List<NonDefaultDay> nonDefaultDays = nonDefaultDaysChangedForHearing.getNonDefaultDays();
        final UUID hearingId = nonDefaultDaysChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObjectList(NON_DEFAULT_DAYS, nonDefaultDays)
                .save();
    }
}

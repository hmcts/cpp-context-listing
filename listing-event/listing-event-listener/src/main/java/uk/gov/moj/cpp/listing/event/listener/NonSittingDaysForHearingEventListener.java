package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

@ServiceComponent(Component.EVENT_LISTENER)
public class NonSittingDaysForHearingEventListener {

    private static final String NON_SITTING_DAYS = "nonSittingDays";

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.non-sitting-days-assigned-to-hearing")
    public void nonSittingDaysAssignedForHearing(final Envelope<NonSittingDaysAssignedToHearing> event) {
        final NonSittingDaysAssignedToHearing nonSittingDaysAssignedToHearing = event.payload();
        final List<LocalDate> nonSittingDays = nonSittingDaysAssignedToHearing.getNonSittingDays();
        final UUID hearingId = nonSittingDaysAssignedToHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putLocalDateList(NON_SITTING_DAYS, nonSittingDays)
                .save();
    }

    @Handles("listing.events.non-sitting-days-changed-for-hearing")
    public void nonSittingDaysChangedForHearing(final Envelope<NonSittingDaysChangedForHearing> event) {
        final NonSittingDaysChangedForHearing nonSittingDaysChangedForHearing =  event.payload();
        final List<LocalDate> nonSittingDays = nonSittingDaysChangedForHearing.getNonSittingDays();
        final UUID hearingId = nonSittingDaysChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putLocalDateList(NON_SITTING_DAYS, nonSittingDays)
                .save();
    }

}

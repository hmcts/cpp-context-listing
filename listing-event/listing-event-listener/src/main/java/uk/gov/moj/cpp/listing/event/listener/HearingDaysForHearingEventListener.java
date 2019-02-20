package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingDaysForHearingEventListener {

    private static final String HEARING_DAYS = "hearingDays";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.hearing-days-changed-for-hearing")
    public void hearingDaysChangedForHearing(final Envelope<HearingDaysChangedForHearing> event) {
        final HearingDaysChangedForHearing hearingDaysChangedForHearing =  event.payload();
        final List<HearingDay> hearingDays = hearingDaysChangedForHearing.getHearingDays();
        final UUID hearingId = hearingDaysChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObjectList(HEARING_DAYS, hearingDays)
                .save();
    }

    @Handles("listing.events.hearing-days-sequenced")
    public void hearingDaysSequenced(final Envelope<HearingDaysSequenced> event) {
        final HearingDaysSequenced sequencedHearing = event.payload();

        final List<HearingDay>  hearingDays = sequencedHearing.getHearingDays();
        final UUID hearingId = sequencedHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObjectList(HEARING_DAYS, hearingDays)
                .save();
    }
}

package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

@Component
public class NonSittingDaysForHearingEventListener implements ListenerPort {

    private static final String NON_SITTING_DAYS = "nonSittingDays";
    private final HearingRepository hearingRepository;

    public NonSittingDaysForHearingEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(NonSittingDaysAssignedToHearing.class, this::nonSittingDaysAssignedForHearing)
                .on(NonSittingDaysChangedForHearing.class, this::nonSittingDaysChangedForHearing);
    }

    public void nonSittingDaysAssignedForHearing(final RaisedEvent<NonSittingDaysAssignedToHearing> event) {
        final NonSittingDaysAssignedToHearing nonSittingDaysAssignedToHearing = event.payload();
        final List<LocalDate> nonSittingDays = nonSittingDaysAssignedToHearing.getNonSittingDays();
        final UUID hearingId = nonSittingDaysAssignedToHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putLocalDateList(NON_SITTING_DAYS, nonSittingDays)
                    .save();
        }
    }
    public void nonSittingDaysChangedForHearing(final RaisedEvent<NonSittingDaysChangedForHearing> event) {
        final NonSittingDaysChangedForHearing nonSittingDaysChangedForHearing =  event.payload();
        final List<LocalDate> nonSittingDays = nonSittingDaysChangedForHearing.getNonSittingDays();
        final UUID hearingId = nonSittingDaysChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putLocalDateList(NON_SITTING_DAYS, nonSittingDays)
                .save();
    }

}

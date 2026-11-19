package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;


@Component
public class NonDefaultDaysForHearingEventListener implements ListenerPort {

    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";
    private final HearingRepository hearingRepository;

    public NonDefaultDaysForHearingEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(NonDefaultDaysAssignedToHearing.class, this::nonDefaultDaysAssignedForHearing)
                .on(NonDefaultDaysChangedForHearing.class, this::nonDefaultDaysChangedForHearing);
    }

    public void nonDefaultDaysAssignedForHearing(final RaisedEvent<NonDefaultDaysAssignedToHearing> event) {
        final NonDefaultDaysAssignedToHearing nonDefaultDaysAssignedToHearing = event.payload();
        final List<NonDefaultDay> nonDefaultDays = nonDefaultDaysAssignedToHearing.getNonDefaultDays();
        final UUID hearingId = nonDefaultDaysAssignedToHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObjectList(NON_DEFAULT_DAYS, nonDefaultDays)
                .save();
    }
    public void nonDefaultDaysChangedForHearing(final RaisedEvent<NonDefaultDaysChangedForHearing> event) {
        final NonDefaultDaysChangedForHearing nonDefaultDaysChangedForHearing =  event.payload();
        final List<NonDefaultDay> nonDefaultDays = nonDefaultDaysChangedForHearing.getNonDefaultDays();
        final UUID hearingId = nonDefaultDaysChangedForHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putObjectList(NON_DEFAULT_DAYS, nonDefaultDays)
                    .save();
        }
    }
}

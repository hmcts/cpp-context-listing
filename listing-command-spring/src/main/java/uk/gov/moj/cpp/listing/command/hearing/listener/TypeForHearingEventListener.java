package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.Type;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;


@Component
public class TypeForHearingEventListener implements ListenerPort {

    private static final String TYPE_FIELD = "type";
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public TypeForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(TypeChangedForHearing.class, this::typeChangedForHearing);
    }

    public void typeChangedForHearing(final RaisedEvent<TypeChangedForHearing> event) {
        final TypeChangedForHearing typeChangedForHearing =   event.payload();
        final Type type = typeChangedForHearing.getType();
        final UUID hearingId = typeChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObject(TYPE_FIELD, type)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}

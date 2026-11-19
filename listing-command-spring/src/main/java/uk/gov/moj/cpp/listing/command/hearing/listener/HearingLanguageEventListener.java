package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;


@Component
public class HearingLanguageEventListener implements ListenerPort {

    private static final String HEARING_LANGUAGE_FIELD = "hearingLanguage";

    private HearingRepository hearingRepository;
    public HearingLanguageEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(HearingLanguageChangedForHearing.class, this::hearingLanguageChanged);
    }

    public void hearingLanguageChanged(final RaisedEvent<HearingLanguageChangedForHearing> event) {
        final HearingLanguageChangedForHearing payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final String hearingLanguage = payload.getHearingLanguage().toString();
        using(hearingRepository)
                .find(hearingId)
                .put(HEARING_LANGUAGE_FIELD, hearingLanguage)
                .save();
    }
}

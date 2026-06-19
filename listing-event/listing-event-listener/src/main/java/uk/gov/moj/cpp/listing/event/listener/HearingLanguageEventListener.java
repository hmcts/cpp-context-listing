package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingLanguageEventListener {

    private static final String HEARING_LANGUAGE_FIELD = "hearingLanguage";

    private HearingRepository hearingRepository;

    @Inject
    public HearingLanguageEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.hearing-language-changed-for-hearing")
    public void hearingLanguageChanged(final Envelope<HearingLanguageChangedForHearing> event) {
        final HearingLanguageChangedForHearing payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final String hearingLanguage = payload.getHearingLanguage().toString();
        using(hearingRepository)
                .find(hearingId)
                .put(HEARING_LANGUAGE_FIELD, hearingLanguage)
                .save();
    }
}

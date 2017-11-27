package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.JudgeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.JudgeChangedForHearing;
import uk.gov.moj.cpp.listing.event.JudgeRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class JudgeForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.events.judge-assigned-to-hearing")
    public void judgeAssignedToHearing(final JsonEnvelope event) {
        final JudgeAssignedToHearing judgeAssignedToHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), JudgeAssignedToHearing.class);
        final UUID judgeId = UUID.fromString(judgeAssignedToHearing.getJudgeId());
        final UUID hearingId = UUID.fromString(judgeAssignedToHearing.getHearingId());
        hearingRepository.updateJudgeId(judgeId, hearingId);
    }

    @Handles("listing.events.judge-changed-for-hearing")
    public void judgeChangedForHearing(final JsonEnvelope event) {
        final JudgeChangedForHearing judgeChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), JudgeChangedForHearing.class);
        final UUID judgeId = UUID.fromString(judgeChangedForHearing.getJudgeId());
        final UUID hearingId = UUID.fromString(judgeChangedForHearing.getHearingId());
        hearingRepository.updateJudgeId(judgeId, hearingId);
    }

    @Handles("listing.events.judge-removed-from-hearing")
    public void judgeRemovedFromHearing(final JsonEnvelope event) {
        final JudgeRemovedFromHearing judgeRemovedFromHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), JudgeRemovedFromHearing.class);
        final UUID hearingId = UUID.fromString(judgeRemovedFromHearing.getHearingId());
        hearingRepository.updateJudgeId(null, hearingId);
    }
}

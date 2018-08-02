package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.JudgeAssignedToHearing;
import uk.gov.justice.listing.events.JudgeChangedForHearing;
import uk.gov.justice.listing.events.JudgeRemovedFromHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class JudgeForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.judge-assigned-to-hearing")
    public void judgeAssignedToHearing(final Envelope<JudgeAssignedToHearing> event) {
        final JudgeAssignedToHearing judgeAssignedToHearing = event.payload();
        final UUID judgeId = judgeAssignedToHearing.getJudgeId();
        final UUID hearingId = judgeAssignedToHearing.getHearingId();
        hearingRepository.updateJudgeId(judgeId, hearingId);
    }

    @Handles("listing.events.judge-changed-for-hearing")
    public void judgeChangedForHearing(final Envelope<JudgeChangedForHearing> event) {
        final JudgeChangedForHearing judgeChangedForHearing  = event.payload();
        final UUID judgeId = judgeChangedForHearing.getJudgeId();
        final UUID hearingId = judgeChangedForHearing.getHearingId();
        hearingRepository.updateJudgeId(judgeId, hearingId);
    }

    @Handles("listing.events.judge-removed-from-hearing")
    public void judgeRemovedFromHearing(final Envelope<JudgeRemovedFromHearing> event) {
        final JudgeRemovedFromHearing judgeRemovedFromHearing  = event.payload();
        final UUID hearingId = judgeRemovedFromHearing.getHearingId();
        hearingRepository.updateJudgeId(null, hearingId);
    }
}

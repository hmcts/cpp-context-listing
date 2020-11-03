package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing;
import uk.gov.justice.listing.events.VideoLinkChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class VideoLinkDetailsEventListener {

    private static final String HAS_VIDEO_LINK = "hasVideoLink";
    private static final String PUBLIC_LIST_NOTE = "publicListNote";
    private HearingRepository hearingRepository;

    @Inject
    public VideoLinkDetailsEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.video-link-details-assigned-for-hearing")
    public void videoLinkDetailsAssigned(final Envelope<VideoLinkDetailsAssignedForHearing> event) {
        final VideoLinkDetailsAssignedForHearing payload = event.payload();
        saveVideoLinkDetail(payload.getHearingId(), payload.getHasVideoLink(), payload.getVideoLinkDetails().orElse(null));
    }

    @Handles("listing.events.video-link-details-changed-for-hearing")
    public void videoLinkDetailsChangedForHearing(final Envelope<VideoLinkDetailsChangedForHearing> event) {
        final VideoLinkDetailsChangedForHearing payload = event.payload();
        saveVideoLinkDetail(payload.getHearingId(), payload.getHasVideoLink(), payload.getVideoLinkDetails().orElse(null));
    }

    @Handles("listing.events.video-link-details-removed-for-hearing")
    public void videoLinkDetailsRemovedFromHearing(final Envelope<VideoLinkDetailsRemovedForHearing> event) {
        final VideoLinkDetailsRemovedForHearing payload = event.payload();
        final UUID hearingId = payload.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .remove(HAS_VIDEO_LINK)
                .remove(PUBLIC_LIST_NOTE)
                .save();
    }

    private void saveVideoLinkDetail(final UUID hearingId, final Boolean hasVideoLink, final String videoLinkDetails) {
        using(hearingRepository)
                .find(hearingId)
                .put(HAS_VIDEO_LINK, hasVideoLink)
                .put(PUBLIC_LIST_NOTE, videoLinkDetails)
                .save();
    }


    @Handles("listing.events.public-list-note-changed-for-hearing")
    public void publicListNoteChangedForHearing(final Envelope<PublicListNoteChangedForHearing> event) {
        final PublicListNoteChangedForHearing payload = event.payload();
        using(hearingRepository)
                .find(payload.getHearingId())
                .put(PUBLIC_LIST_NOTE, payload.getPublicListNote())
                .save();
    }

    @Handles("listing.events.public-list-note-removed-from-hearing")
    public void publicListNoteRemovedForHearing(final Envelope<PublicListNoteRemovedFromHearing> event) {
        final PublicListNoteRemovedFromHearing payload = event.payload();
        using(hearingRepository)
                .find(payload.getHearingId())
                .remove(PUBLIC_LIST_NOTE)
                .save();
    }

    @Handles("listing.events.video-link-changed-for-hearing")
    public void videoLinkChangedForHearing(final Envelope<VideoLinkChangedForHearing> event) {
        final VideoLinkChangedForHearing payload = event.payload();
        using(hearingRepository)
                .find(payload.getHearingId())
                .put(HAS_VIDEO_LINK, payload.getHasVideoLink())
                .save();
    }

}

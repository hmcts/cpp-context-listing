package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing;
import uk.gov.justice.listing.events.VideoLinkChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;


@Component
public class VideoLinkDetailsEventListener implements ListenerPort {

    private static final String HAS_VIDEO_LINK = "hasVideoLink";
    private static final String PUBLIC_LIST_NOTE = "publicListNote";
    private HearingRepository hearingRepository;
    public VideoLinkDetailsEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(VideoLinkDetailsAssignedForHearing.class, this::videoLinkDetailsAssigned)
                .on(VideoLinkDetailsChangedForHearing.class, this::videoLinkDetailsChangedForHearing)
                .on(VideoLinkDetailsRemovedForHearing.class, this::videoLinkDetailsRemovedFromHearing)
                .on(PublicListNoteChangedForHearing.class, this::publicListNoteChangedForHearing)
                .on(PublicListNoteRemovedFromHearing.class, this::publicListNoteRemovedForHearing)
                .on(VideoLinkChangedForHearing.class, this::videoLinkChangedForHearing);
    }

    public void videoLinkDetailsAssigned(final RaisedEvent<VideoLinkDetailsAssignedForHearing> event) {
        final VideoLinkDetailsAssignedForHearing payload = event.payload();
        saveVideoLinkDetail(payload.getHearingId(), payload.getHasVideoLink(), payload.getVideoLinkDetails());
    }
    public void videoLinkDetailsChangedForHearing(final RaisedEvent<VideoLinkDetailsChangedForHearing> event) {
        final VideoLinkDetailsChangedForHearing payload = event.payload();
        saveVideoLinkDetail(payload.getHearingId(), payload.getHasVideoLink(), payload.getVideoLinkDetails());
    }
    public void videoLinkDetailsRemovedFromHearing(final RaisedEvent<VideoLinkDetailsRemovedForHearing> event) {
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
    public void publicListNoteChangedForHearing(final RaisedEvent<PublicListNoteChangedForHearing> event) {
        final PublicListNoteChangedForHearing payload = event.payload();
        using(hearingRepository)
                .find(payload.getHearingId())
                .put(PUBLIC_LIST_NOTE, payload.getPublicListNote())
                .save();
    }
    public void publicListNoteRemovedForHearing(final RaisedEvent<PublicListNoteRemovedFromHearing> event) {
        final PublicListNoteRemovedFromHearing payload = event.payload();
        using(hearingRepository)
                .find(payload.getHearingId())
                .remove(PUBLIC_LIST_NOTE)
                .save();
    }
    public void videoLinkChangedForHearing(final RaisedEvent<VideoLinkChangedForHearing> event) {
        final VideoLinkChangedForHearing payload = event.payload();

        if (nonNull(hearingRepository.findBy(payload.getHearingId()))) {
            using(hearingRepository)
                    .find(payload.getHearingId())
                    .put(HAS_VIDEO_LINK, payload.getHasVideoLink())
                    .save();
        }
    }

}

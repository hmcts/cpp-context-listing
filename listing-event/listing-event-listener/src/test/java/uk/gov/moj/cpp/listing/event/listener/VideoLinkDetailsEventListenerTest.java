package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VideoLinkDetailsEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final Boolean HAS_VIDEO_LINK = true;
    private static final String VIDEO_LINK_DETAILS = "videoLinkDetails";
    private static final String JSON_PATH = "{\"test\": \"test\"}";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    Hearing hearing;

    @InjectMocks
    private VideoLinkDetailsEventListener videoLinkDetailsEventListener;

    @Test
    public void shouldAssignVideoLinkDetailsToHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(JSON_PATH);

        Envelope<VideoLinkDetailsAssignedForHearing> envelope = (Envelope<VideoLinkDetailsAssignedForHearing>) mock(Envelope.class);
        VideoLinkDetailsAssignedForHearing hearingData = VideoLinkDetailsAssignedForHearing.videoLinkDetailsAssignedForHearing()
                .withHasVideoLink(HAS_VIDEO_LINK)
                .withVideoLinkDetails(VIDEO_LINK_DETAILS)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        videoLinkDetailsEventListener.videoLinkDetailsAssigned(envelope);

        verify(hearingRepository).save(hearing);

    }

    @Test
    public void shouldChangeVideoLinkDetailsForHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(JSON_PATH);

        Envelope<VideoLinkDetailsChangedForHearing> envelope = (Envelope<VideoLinkDetailsChangedForHearing>) mock(Envelope.class);
        VideoLinkDetailsChangedForHearing hearingData = VideoLinkDetailsChangedForHearing.videoLinkDetailsChangedForHearing()
                .withHasVideoLink(HAS_VIDEO_LINK)
                .withVideoLinkDetails(VIDEO_LINK_DETAILS)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        videoLinkDetailsEventListener.videoLinkDetailsChangedForHearing(envelope);

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveVideoLinkDetailsFromHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(JSON_PATH);

        Envelope<VideoLinkDetailsRemovedForHearing> envelope = (Envelope<VideoLinkDetailsRemovedForHearing>) mock(Envelope.class);
        VideoLinkDetailsRemovedForHearing hearingData = VideoLinkDetailsRemovedForHearing.videoLinkDetailsRemovedForHearing()
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        videoLinkDetailsEventListener.videoLinkDetailsRemovedFromHearing(envelope);

        verify(hearingRepository).save(hearing);
    }

}

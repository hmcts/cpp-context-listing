package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing;
import uk.gov.justice.listing.events.VideoLinkChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VideoLinkDetailsEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();

    private ObjectMapper objectMapper;

    @Mock
    private HearingRepository hearingRepository;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @InjectMocks
    private VideoLinkDetailsEventListener videoLinkDetailsEventListener;

    @BeforeEach
    public void setup(){
        objectMapper= new ObjectMapper();
    }

    @Test
    public void shouldAssignVideoLinkDetailsToHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        final String existedPayload = "{}";
        final String expectedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"public list note\"}";
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<VideoLinkDetailsAssignedForHearing> envelope = (Envelope<VideoLinkDetailsAssignedForHearing>) mock(Envelope.class);
        final VideoLinkDetailsAssignedForHearing hearingData = VideoLinkDetailsAssignedForHearing.videoLinkDetailsAssignedForHearing()
                .withHasVideoLink(true)
                .withVideoLinkDetails("public list note")
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());

        videoLinkDetailsEventListener.videoLinkDetailsAssigned(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));

    }

    @Test
    public void shouldChangeVideoLinkDetailsForHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        final String existedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"public list note\"}";
        final String expectedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"public list note change\"}";
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<VideoLinkDetailsChangedForHearing> envelope = (Envelope<VideoLinkDetailsChangedForHearing>) mock(Envelope.class);
        final  VideoLinkDetailsChangedForHearing hearingData = VideoLinkDetailsChangedForHearing.videoLinkDetailsChangedForHearing()
                .withHasVideoLink(true)
                .withVideoLinkDetails("public list note change")
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());

        videoLinkDetailsEventListener.videoLinkDetailsChangedForHearing(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));
    }

    @Test
    public void shouldRemoveVideoLinkDetailsFromHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        final String existedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"public list note\"}";
        final String expectedPayload = "{}";
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<VideoLinkDetailsRemovedForHearing> envelope = (Envelope<VideoLinkDetailsRemovedForHearing>) mock(Envelope.class);
        final VideoLinkDetailsRemovedForHearing hearingData = VideoLinkDetailsRemovedForHearing.videoLinkDetailsRemovedForHearing()
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());

        videoLinkDetailsEventListener.videoLinkDetailsRemovedFromHearing(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));
    }


    @Test
    public void shouldPublicListNoteChangedForHearing() throws Exception {
        final String existedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"test\"}";
        final String expectedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"change test\"}";
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<PublicListNoteChangedForHearing> envelope = (Envelope<PublicListNoteChangedForHearing>) mock(Envelope.class);
        final PublicListNoteChangedForHearing hearingData = PublicListNoteChangedForHearing.publicListNoteChangedForHearing()
                .withPublicListNote("change test")
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());


        videoLinkDetailsEventListener.publicListNoteChangedForHearing(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));
    }

    @Test
    public void shouldPublicListNoteRemovedForHearing() throws Exception {
        final String existedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"test\"}";
        final String expectedPayload = "{\"hasVideoLink\":true}" ;
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<PublicListNoteRemovedFromHearing> envelope = (Envelope<PublicListNoteRemovedFromHearing>) mock(Envelope.class);
        final PublicListNoteRemovedFromHearing hearingData = PublicListNoteRemovedFromHearing.publicListNoteRemovedFromHearing()

                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());


        videoLinkDetailsEventListener.publicListNoteRemovedForHearing(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));
    }

    @Test
    public void shouldVideoLinkChangedForHearing() throws Exception {

        final String existedPayload = "{\"hasVideoLink\":true," +
                "\"publicListNote\":\"test\"}";
        final String expectedPayload = "{\"hasVideoLink\":false," +
                "\"publicListNote\":\"test\"}";
        final ObjectNode properties = (ObjectNode) objectMapper.readTree(existedPayload);

        final Envelope<VideoLinkChangedForHearing> envelope = (Envelope<VideoLinkChangedForHearing>) mock(Envelope.class);
        final VideoLinkChangedForHearing hearingData = VideoLinkChangedForHearing.videoLinkChangedForHearing()
                .withHasVideoLink(false)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(Hearing.builder()
                .withProperties(properties)
                .build());


        videoLinkDetailsEventListener.videoLinkChangedForHearing(envelope);

        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        assertThat(expectedPayload,is(objectMapper.writeValueAsString(hearingArgumentCaptor.getValue().getProperties())));
    }

}

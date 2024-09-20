package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.listing.event.listener.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.event.listener.utils.FileUtil.givenPayload;

import uk.gov.justice.listing.event.HearingCounselModified;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingCounselEventListenerTest {


    @Mock
    private HearingRepository hearingRepository;

    @Test
    public void shouldAddDefenceCounsel() throws Exception {
        verifyCounselAction(null,
                "/listing.event.hearing-counsel-modified.json",
                "hearing-counsel-modified-properties.json");
    }

    @Test
    public void shouldUpdateDefenceCounsel() throws Exception {
        verifyCounselAction("hearing-with-defence-counsels.json",
                "/listing.event.hearing-counsel-modified-updated.json",
                "hearing-counsel-modified-action-update-properties.json");
    }

    @Test
    public void shouldRemoveDefenceCounsel() throws Exception {
        verifyCounselAction("hearing-with-defence-counsels.json", "/listing.event.hearing-counsel-removed.json",
                "hearing-counsel-modified-action-remove-properties.json");
    }

    private void verifyCounselAction(final String existingHearing, final String counselModifiedEvent, final String expectedHearing) throws IOException {
        final Envelope<HearingCounselModified> envelope = (Envelope<HearingCounselModified>) mock(Envelope.class);
        JsonObject payload = givenPayload(counselModifiedEvent);

        UUID hearingId = fromString(payload.getString("hearingId"));
        final HearingCounselModified hearingCounselModified = new HearingCounselModified(
                uk.gov.justice.listing.event.Action.valueFor(payload.getString("action")).orElse(null),
                uk.gov.justice.listing.event.CounselType.valueFor(payload.getString("counselType")).orElse(null),
                hearingId,
                payload.getString("payload"));

        final Hearing hearing = spy(Hearing.class);
        hearing.setId(hearingId);
        final ObjectMapper objectMapper = new ObjectMapper();
        if (isNull(existingHearing)) {
            hearing.setProperties(objectMapper.createObjectNode());
        } else {
            hearing.setProperties(objectMapper.readTree(getPayload(existingHearing)));
        }

        given(envelope.payload()).willReturn(hearingCounselModified);
        given(hearingRepository.findBy(hearingId)).willReturn(hearing);

        final ArgumentCaptor<Hearing> hearingArgumentCaptor =
                ArgumentCaptor.forClass(Hearing.class);

        HearingCounselEventListener hearingCounselEventListener = new HearingCounselEventListener(hearingRepository, objectMapper);
        hearingCounselEventListener.hearingCounselModified(envelope);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingId));

        JsonNode expectedProperties = objectMapper.readTree(getPayload(expectedHearing));
        assertThat(hearingArgumentCaptor.getValue().getProperties(), equalTo(expectedProperties));
    }
}

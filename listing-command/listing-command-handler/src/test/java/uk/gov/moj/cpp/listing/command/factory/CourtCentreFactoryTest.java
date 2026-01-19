package uk.gov.moj.cpp.listing.command.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.utils.FileUtil;

import java.io.StringReader;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CourtCentreFactoryTest {
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_UUID_1 = UUID.randomUUID();
    private static final UUID COURT_ROOM_UUID_2 = UUID.randomUUID();

    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);
    private static final String  DEFAULT_DURATION_HOURS_MINS = "6:30";

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CourtCentreFactory courtCentreFactory;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;


    @Test
    public void shouldReturnCourtCentre() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        final JsonObject jsonEnvelope = getJsonEnvelope();
        given(finalEnvelope.payloadAsJsonObject()).willReturn(jsonEnvelope);

        //when
        final JsonObject organisationUnit = courtCentreFactory.getOrganisationUnit(COURT_CENTRE_ID, envelope);

        //then
        verify(referenceDataService).getCourtCentreById(uuidArgumentCaptor.capture(), jsonEnvelopeArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue(), is(COURT_CENTRE_ID));
        assertThat(jsonEnvelopeArgumentCaptor.getValue(), is(envelope));

    }


    @Test
    public void shouldReturnCourtRoomNumber() {

        //given
        final JsonObject jsonEnvelope = getJsonEnvelope();


        //when
        final Optional<Integer> courtRoomNumber = courtCentreFactory.getCourtRoomNumber(jsonEnvelope, COURT_ROOM_UUID_1.toString());

        //then
        assertThat(courtRoomNumber.isPresent(), is(true));
        assertThat(courtRoomNumber.get(), is(294));

    }


    private JsonObject getJsonEnvelope() {
        return getJsonEnvelope(DEFAULT_TIME.toString(), DEFAULT_DURATION_HOURS_MINS, COURT_CENTRE_ID.toString());
    }

    private JsonObject getJsonEnvelope(String defaultTime, String defaultDurationHours, String courtCentreId) {
        String jsonString = FileUtil.getPayload("/stub-data/stubbed.referencedata.query.courtroom.json")
                .replace("DEFAULT_START_TIME", defaultTime)
                .replace("DEFAULT_DURATION_HOURS_MINS", defaultDurationHours)
                .replace("COURT_CENTRE_ID", courtCentreId)
                .replace("COURT_ROOM_UUID_1", COURT_ROOM_UUID_1.toString())
                .replace("COURT_ROOM_UUID_2", COURT_ROOM_UUID_2.toString());

        return JsonObjects.createReader(new StringReader(jsonString)).readObject();
    }

}
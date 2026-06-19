package uk.gov.moj.cpp.listing.command.api.courtcentre;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.api.util.FileUtil;

import java.io.StringReader;
import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtCentreFactoryTest {
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);
    private static final String  DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final String DEFAULT_DURATION_6_HOURS_AND_NO_MINS = "6:00";
    private static final String DEFAULT_DURATION_6_HOURS_AND_NO_COLON = "6";

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private CourtCentreFactory courtCentreFactory;

    @Test
    public void shouldReturnCourtCentre() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getDefaultJsonEnvelope());

        //when
        CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope);

        //then
        assertThat(courtCentre.getDefaultDuration(), is(390));
        assertThat(courtCentre.getDefaultStartTime(), is(DEFAULT_TIME));
    }

    @Test
    public void shouldReturnCourtCentreWithDurationOf6HoursAndNoMinutes() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getDefaultJsonEnvelope(DEFAULT_TIME.toString(), DEFAULT_DURATION_6_HOURS_AND_NO_MINS, COURT_CENTRE_ID.toString()));

        //when
        CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope);

        //then
        assertThat(courtCentre.getDefaultDuration(), is(360));
    }

    @Test
    public void shouldReturnCourtCentreWithDurationOf6HoursAndNoColonAndNoMinutes() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getDefaultJsonEnvelope(DEFAULT_TIME.toString(), DEFAULT_DURATION_6_HOURS_AND_NO_COLON, COURT_CENTRE_ID.toString()));

        //when
        CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope);

        //then
        assertThat(courtCentre.getDefaultDuration(), is(360));
    }

    @Test
    public void shouldReturnIllegalArgumentExceptionWhenNoDefaultTime() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getJsonEnvelopeWithNoDefaultStartTime());

        //when
        assertThrows(IllegalArgumentException.class, () -> courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope));
    }

    private JsonObject getDefaultJsonEnvelope() {
        return getDefaultJsonEnvelope(DEFAULT_TIME.toString(), DEFAULT_DURATION_HOURS_MINS, COURT_CENTRE_ID.toString());
    }

    private JsonObject getDefaultJsonEnvelope(String defaultTime, String defaultDurationHours, String courtCentreId) {
        String jsonString = FileUtil.getPayload("stubbed.referencedata.query.courtroom.json")
                .replace("DEFAULT_START_TIME", defaultTime)
                .replace("DEFAULT_DURATION_HOURS_MINS", defaultDurationHours)
                .replace("COURT_CENTRE_ID", courtCentreId);
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject getJsonEnvelopeWithNoDefaultStartTime() {
        return getDefaultJsonEnvelope(DEFAULT_TIME.toString(), "", COURT_CENTRE_ID.toString());

    }
}
package uk.gov.moj.cpp.listing.event.processor.courtcenter;

import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.event.utils.FileUtil;

import java.io.StringReader;
import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
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
    private Logger logger;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CourtCentreFactory courtCentreFactory;

    @Test
    public void shouldReturnCourtCentre() {

        when(logger.isInfoEnabled()).thenReturn(true);

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getJsonEnvelope());

        //when
        CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope);

        //then
        assertThat(courtCentre.getDefaultDuration(), is(390));
        assertThat(courtCentre.getDefaultStartTime(), is(DEFAULT_TIME));
    }

    private JsonObject getJsonEnvelope() {
        return getJsonEnvelope(DEFAULT_TIME.toString(), DEFAULT_DURATION_HOURS_MINS, COURT_CENTRE_ID.toString());
    }

    private JsonObject getJsonEnvelope(String defaultTime, String defaultDurationHours, String courtCentreId) {
        String jsonString = FileUtil.getPayload("stub-data/stubbed.referencedata.query.courtroom.json")
                .replace("DEFAULT_START_TIME", defaultTime)
                .replace("DEFAULT_DURATION_HOURS_MINS", defaultDurationHours)
                .replace("COURT_CENTRE_ID", courtCentreId);
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

}
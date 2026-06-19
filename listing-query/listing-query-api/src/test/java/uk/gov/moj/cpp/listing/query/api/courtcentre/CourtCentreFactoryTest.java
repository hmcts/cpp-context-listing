package uk.gov.moj.cpp.listing.query.api.courtcentre;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;

import java.io.StringReader;
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
    private static final String COURT_CENTRE_NAME = "courtCentreName";

    private static final String COURT_CENTRE_WELSH_NAME = "courtCentreNameWelsh";
    private static final String ADDRESS_1 = "address_1";
    private static final String ADDRESS_2 = "address_2";
    private static final String ADDRESS_3 = "address_3";
    private static final String ADDRESS_4 = "address_4";
    private static final String ADDRESS_5 = "address_5";
    private static final String POSTCODE = "P0STC013E";
    private static final String WELSH_ADDRESS_1 = "welsh_address_1";
    private static final String WELSH_ADDRESS_2 = "welsh_address_2";
    private static final String WELSH_ADDRESS_3 = "welsh_address_3";
    private static final String WELSH_ADDRESS_4 = "welsh_address_4";
    private static final String WELSH_ADDRESS_5 = "welsh_address_5";

    private static final UUID COURT_ROOM_1_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_2_ID = UUID.randomUUID();
    private static final String COURT_ROOM_NAME_1 = "courtRoomName1";
    private static final String COURT_ROOM_NAME_2 = "courtRoomName2";
    private static final String COURT_ROOM_WELSH_NAME_1 = "courtRoomWelshName1";
    private static final String COURT_ROOM_WELSH_NAME_2 = "courtRoomWelshName2";
    private static final String COURT_START_TIME = "10:30";

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CourtCentreFactory courtCentreFactory;

    @Test
    public void shouldReturnCourtCentre() {

        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getJsonEnvelope());

        //when
        CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope);

        //then
        assertThat(courtCentre.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(courtCentre.getWelshCourtCentreName(), is(COURT_CENTRE_WELSH_NAME));
        assertThat(courtCentre.getPostcode(), is(POSTCODE));
        assertThat(courtCentre.getAddress1(), is(ADDRESS_1));
        assertThat(courtCentre.getAddress2(), is(ADDRESS_2));
        assertThat(courtCentre.getAddress3(), is(ADDRESS_3));
        assertThat(courtCentre.getAddress4(), is(ADDRESS_4));
        assertThat(courtCentre.getAddress5(), is(ADDRESS_5));
        assertThat(courtCentre.getWelshAddress1(), is(WELSH_ADDRESS_1));
        assertThat(courtCentre.getWelshAddress2(), is(WELSH_ADDRESS_2));
        assertThat(courtCentre.getWelshAddress3(), is(WELSH_ADDRESS_3));
        assertThat(courtCentre.getWelshAddress4(), is(WELSH_ADDRESS_4));
        assertThat(courtCentre.getWelshAddress5(), is(WELSH_ADDRESS_5));
        assertThat(courtCentre.getDefaultStartTime(), is(COURT_START_TIME));
        assertThat(courtCentre.isWelsh(), is(true));
        CourtRoomDetails courtRoom1Details = courtCentre.getCourtRooms().get(COURT_ROOM_1_ID);
        assertThat(courtRoom1Details.getCourtRoomName(), is(COURT_ROOM_NAME_1));
        assertThat(courtRoom1Details.getWelshCourtRoomName(), is(COURT_ROOM_WELSH_NAME_1));

        CourtRoomDetails courtRoom2Details = courtCentre.getCourtRooms().get(COURT_ROOM_2_ID);
        assertThat(courtRoom2Details.getCourtRoomName(), is(COURT_ROOM_NAME_2));
        assertThat(courtRoom2Details.getWelshCourtRoomName(), is(COURT_ROOM_WELSH_NAME_2));
    }

    private JsonObject getJsonEnvelope() {
        String jsonString = FileUtil.getPayload("stubbed.referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_NAME", COURT_CENTRE_NAME)
                .replace("COURT_CENTRE_WELSH_NAME", COURT_CENTRE_WELSH_NAME)
                .replace("COURT_START_TIME", COURT_START_TIME)
                .replace("ADDRESS_1", ADDRESS_1)
                .replace("ADDRESS_2", ADDRESS_2)
                .replace("ADDRESS_3", ADDRESS_3)
                .replace("ADDRESS_4", ADDRESS_4)
                .replace("ADDRESS_5", ADDRESS_5)
                .replace("POSTCODE", POSTCODE)
                .replace("ADDRESS_WELSH_1", WELSH_ADDRESS_1)
                .replace("ADDRESS_WELSH_2", WELSH_ADDRESS_2)
                .replace("ADDRESS_WELSH_3", WELSH_ADDRESS_3)
                .replace("ADDRESS_WELSH_4", WELSH_ADDRESS_4)
                .replace("ADDRESS_WELSH_5", WELSH_ADDRESS_5)
                .replace("COURT_ROOM_1_ID", COURT_ROOM_1_ID.toString())
                .replace("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString())
                .replace("COURT_ROOM_1_NAME", COURT_ROOM_NAME_1)
                .replace("COURT_ROOM_WELSH_NAME_1", COURT_ROOM_WELSH_NAME_1)
                .replace("COURT_ROOM_2_NAME", COURT_ROOM_NAME_2)
                .replace("COURT_ROOM_WELSH_NAME_2", COURT_ROOM_WELSH_NAME_2)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString());

        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}

package uk.gov.moj.cpp.listing.event.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.event.CourtRoomAdded;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class CourtRoomConverterTest {

    @InjectMocks
    private CourtRoomConverter courtRoomConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldConvertToCourtRoom() throws Exception {

        CourtRoomAdded event = new CourtRoomAdded(randomUUID().toString(), STRING.next(), STRING.next());

        uk.gov.moj.cpp.listing.persistence.entity.CourtRoom courtRoom = courtRoomConverter.convert(event);

        assertThat(courtRoom.getId().toString(), is(event.getId().toString()));
        assertThat(courtRoom.getCourtCentre(), is(event.getCourtCentre()));
        assertThat(courtRoom.getCourtRoomName(), is(event.getCourtRoomName()));
    }

}
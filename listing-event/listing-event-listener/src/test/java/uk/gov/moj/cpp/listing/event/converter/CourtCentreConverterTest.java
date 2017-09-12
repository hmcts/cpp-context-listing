package uk.gov.moj.cpp.listing.event.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.event.CourtCentreAdded;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class CourtCentreConverterTest {

    @InjectMocks
    private CourtCentreConverter courtCentreConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldConvertToCourtCentre() throws Exception {

        CourtCentreAdded event = new CourtCentreAdded(randomUUID().toString(), STRING.next());

        uk.gov.moj.cpp.listing.persistence.entity.CourtCentre courtCentre = courtCentreConverter.convert(event);

        assertThat(courtCentre.getId().toString(), is(event.getId().toString()));
        assertThat(courtCentre.getCourtCentreName(), is(event.getCourtCentreName()));
    }

}
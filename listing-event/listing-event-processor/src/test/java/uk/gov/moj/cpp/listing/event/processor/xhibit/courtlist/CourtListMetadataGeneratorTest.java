package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtListMetadataGeneratorTest {

    @InjectMocks
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Mock
    private XhibitReferenceDataService xhibitReferenceDataService;

    @Spy
    private Clock clock = new StoppedClock(parse("2018-01-02T13:04:05+00:00[Europe/London]"));

    @Test
    public void shouldGenerateMetadataForFirmList() {

        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final UUID courtCentreId = UUID.randomUUID();
        final String crestCourtId = "421";
        final CourtLocation courtLocation = new CourtLocation(
                null,
                crestCourtId,
                null,
                null,
                null,
                null,
                null,
                "CROWN_COURT");

        when(xhibitReferenceDataService.getCourtDetails(envelope, courtCentreId)).thenReturn(courtLocation);

        final PublishCourtListRequestParameters requestParameters = PublishCourtListRequestParametersBuilder
                .withDefaults()
                .withCourtCentreId(courtCentreId)
                .publishCourtListType(PublishCourtListType.FIRM)
                .build();

        final CourtListMetadata metadata = courtListMetadataGenerator.generate(envelope, requestParameters);

        // Validate filename
        final String[] filenamePart = metadata.getFilename().split("_");

        assertThat(filenamePart[0], is("FirmedList"));
        assertThat(filenamePart[1], is(crestCourtId));
        assertThat(filenamePart[2], is("180102130405.xml"));
    }
}

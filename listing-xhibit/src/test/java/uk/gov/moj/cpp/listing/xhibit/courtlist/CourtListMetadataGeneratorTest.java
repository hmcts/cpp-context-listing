package uk.gov.moj.cpp.listing.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtListMetadataGeneratorTest {

    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    private final Clock clock = Clock.fixed(parse("2018-01-02T13:04:05+00:00[Europe/London]").toInstant(), java.time.ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        courtListMetadataGenerator = new CourtListMetadataGenerator(commonXhibitReferenceDataService, clock);
    }

    @Test
    public void shouldGenerateMetadataForFirmList() {

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

        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId)).thenReturn(courtLocation);

        final PublishCourtListRequestParameters requestParameters = PublishCourtListRequestParametersBuilder
                .withDefaults()
                .withCourtCentreId(courtCentreId)
                .publishCourtListType(PublishCourtListType.FIRM)
                .build();

        final CourtListMetadata metadata = courtListMetadataGenerator.generate(requestParameters);

        // Validate filename
        final String[] filenamePart = metadata.getFilename().split("_");

        assertThat(filenamePart[0], is("FirmList"));
        assertThat(filenamePart[1], is(crestCourtId));
        assertThat(filenamePart[2], is("20180102130405.xml"));
    }
}

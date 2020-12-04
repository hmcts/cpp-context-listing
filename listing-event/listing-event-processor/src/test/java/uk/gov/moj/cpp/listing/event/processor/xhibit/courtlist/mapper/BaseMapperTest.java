package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.time.ZonedDateTime.parse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import javax.json.JsonObject;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

public abstract class BaseMapperTest {

    @Spy
    protected XmlUtils xmlUtils;

    @Mock
    protected CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Mock
    protected JsonEnvelope envelope;
    protected PublishCourtListRequestParameters requestParameters;
    protected CourtListMetadata metadata;
    protected CourtListGenerationContext context;
    @Mock
    private Logger logger;

    @Before
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
    }

    @Before
    public void mockDataSources() {

        MockitoAnnotations.initMocks(this);

        final CourtLocation crownCourtLocation = new CourtLocation(
                "OUCODE",
                "000",
                "001",
                "MOCK_CROWN_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME",
                "MOCKSITECODE",
                "CROWN_COURT");

        final CourtLocation magsCourtLocation = new CourtLocation(
                "OUCODE",
                "000",
                "001",
                "MOCK_MAGISTRATES_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME",
                "000",
                "MAGISTRATES_COURT");

        when(commonXhibitReferenceDataService.getCrownCourtDetails(any())).thenReturn(crownCourtLocation);
        when(commonXhibitReferenceDataService.getMagsCourtDetails(any())).thenReturn(magsCourtLocation);

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(commonXhibitReferenceDataService.getJudiciary(any())).thenReturn(judiciary);

        final HearingType hearingType = new HearingType.Builder()
                .withExhibitHearingCode("XXX")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION")
                .build();
        when(commonXhibitReferenceDataService.getXhibitHearingType(any())).thenReturn(hearingType);

        requestParameters = withDefaults()
                .build();

        metadata = new CourtListMetadata(requestParameters.getPublishCourtListType().name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        context = new CourtListGenerationContext(envelope, requestParameters, metadata);
    }
}

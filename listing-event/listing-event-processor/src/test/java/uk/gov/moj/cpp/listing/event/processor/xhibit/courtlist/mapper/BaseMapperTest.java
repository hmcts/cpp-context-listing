package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.time.ZonedDateTime.parse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import javax.json.Json;
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
    protected XhibitReferenceDataService xhibitReferenceDataService;

    @Mock
    protected JsonEnvelope envelope;

    @Mock
    private Logger logger;

    protected PublishCourtListRequestParameters requestParameters;
    protected CourtListMetadata metadata;
    protected CourtListGenerationContext context;

    @Before
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
    }

    @Before
    public void mockDataSources() {

        MockitoAnnotations.initMocks(this);

        final CourtLocation courtLocation = new CourtLocation("001","000","MOCKCOURTNAME",
                "MOCK", "MOCKSITECODE", "CROWN_COURT");
        when(xhibitReferenceDataService.getCourtDetails(any(), any())).thenReturn(courtLocation);

        final JsonObject judge = givenPayload("/xhibit/mock-data/referencedata.query.get.judge.json");
        when(xhibitReferenceDataService.getJudge(any(), any())).thenReturn(judge);

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(xhibitReferenceDataService.getJudiciary(any(), any())).thenReturn(judiciary);

        final JsonObject hearingType = Json.createObjectBuilder()
                .add("hearingCode", "XXX")
                .add("hearingDescription", "XHIBIT_HEARING_DESCRIPTION")
                .build();
        when(xhibitReferenceDataService.getXhibitHearingType(any(), any())).thenReturn(hearingType);

        requestParameters = withDefaults()
                .build();

        metadata = new CourtListMetadata(requestParameters.getPublishCourtListType().name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        context = new CourtListGenerationContext(envelope, requestParameters, metadata);
    }
}

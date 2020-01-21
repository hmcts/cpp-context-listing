package uk.gov.moj.cpp.listing.query.view.courtlist;


import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.compareJson;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.getJsonFile;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.prettifyJson;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

@RunWith(Parameterized.class)
public class RangeSearchConverterTest {

    private static final Logger LOGGER = getLogger(RangeSearchConverterTest.class);

    @Parameterized.Parameter(0)
    public String rangeSearchResponseFilename;
    @Parameterized.Parameter(1)
    public String expectedCourtListFilename;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private XhibitReferenceDataService xhibitReferenceDataService;
    @InjectMocks
    private RangeSearchConverter rangeSearchConverter;

    @Parameterized.Parameters(name = "{index}: Test with ={0}, expectedCourtListFilename is:{1} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"courtlist/0.wc-single-hearing/range-search-response.json", "courtlist/0.wc-single-hearing/expected-court-list.json"},
                {"courtlist/1.fixed-date-multiple-hearings/range-search-response.json", "courtlist/1.fixed-date-multiple-hearings/expected-court-list.json"},
                {"courtlist/2.fixed-date-multiple-sittings/range-search-response.json", "courtlist/2.fixed-date-multiple-sittings/expected-court-list.json"},
                {"courtlist/3.wc-unallocated/range-search-response.json", "courtlist/3.wc-unallocated/expected-court-list.json"},
                {"courtlist/4.case-with-multiple-days/range-search-response.json", "courtlist/4.case-with-multiple-days/expected-court-list.json"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGenerateExpectedCourtListResponse() throws IOException {

        final JsonObject rangeSearchResponse = getJsonFile(rangeSearchResponseFilename);

        final UUID courtCentreId = fromString("eeb81654-eb5b-443f-ad4b-911606732e53");

        final List<JsonObject> courtSites = Collections.singletonList(
                Json.createObjectBuilder()
                        .add("crestCourtSiteId", "001")
                        .add("crestCourtSiteName", "SITENAME")
                        .add("courtType", "CROWN_COURT")
                        .build());

        when(xhibitReferenceDataService.getCrestCourtSitesForCourtCentre(envelope, courtCentreId)).thenReturn(courtSites);

        final JsonObject generatedCourtList = rangeSearchConverter.generateCourtListQueryPayload(envelope, courtCentreId, rangeSearchResponse);

        final JsonObject expectedCourtList = getJsonFile(expectedCourtListFilename);

        assertThat(generatedCourtList, is(notNullValue()));

        LOGGER.info(prettifyJson(generatedCourtList));

        compareJson(generatedCourtList, expectedCourtList);
    }
}

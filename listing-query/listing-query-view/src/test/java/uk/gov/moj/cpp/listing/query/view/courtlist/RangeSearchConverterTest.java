package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.compareJson;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.getJsonFile;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

// FIXME!!! Temporarily using lenient strictness to get this
// context running with junit 5.
@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class RangeSearchConverterTest {

    private static final UUID COURT_SITE_A_COURT_ROOM_ID = UUID.fromString("5e1c7b54-3bca-3a37-a85a-84510f115b76");
    private static final UUID COURT_SITE_A_COURT_ROOM_ID_3 = UUID.fromString("3e1c7b54-3bca-3a37-a85a-84510f115b33");
    private static final UUID COURT_SITE_B_COURT_ROOM_ID = UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39");
    private static final UUID UNKNOWN_COURT_SITE_COURT_ROOM_ID = UUID.fromString("6508af42-e4d4-396d-a752-d676ebd38f6d");

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @InjectMocks
    private RangeSearchConverter rangeSearchConverter;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("courtlist/0.wc-single-hearing/range-search-response.json", "courtlist/0.wc-single-hearing/expected-court-list.json", null),
                Arguments.of("courtlist/1.fixed-date-multiple-hearings/range-search-response.json", "courtlist/1.fixed-date-multiple-hearings/expected-court-list.json", null),
                Arguments.of("courtlist/2.fixed-date-multiple-sittings/range-search-response.json", "courtlist/2.fixed-date-multiple-sittings/expected-court-list.json", null),
                Arguments.of("courtlist/3.wc-unallocated/range-search-response.json", "courtlist/3.wc-unallocated/expected-court-list.json", null),
                Arguments.of("courtlist/4.case-with-multiple-days/range-search-response.json", "courtlist/4.case-with-multiple-days/expected-court-list.json", "2019-12-27"),
                Arguments.of("courtlist/5.case-with-multiple-days-ste/range-search-response.json", "courtlist/5.case-with-multiple-days-ste/expected-court-list.json", "2020-02-21")
        );
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldGenerateExpectedCourtListResponse(
            final String rangeSearchResponseFilename,
            final String expectedCourtListFilename,
            final String endDate) throws Exception {

        final JsonObject rangeSearchResponse = getJsonFile(rangeSearchResponseFilename);

        final UUID courtCentreId = fromString("eeb81654-eb5b-443f-ad4b-911606732e53");

        final List<JsonObject> courtSites = Arrays.asList(
                buildCourtSite("A"), buildCourtSite("B"));

        final Optional<CourtRoomMapping> courtRoom1 = courtRoom("A");
        final Optional<CourtRoomMapping> courtRoom2 = courtRoom("B");
        final Optional<CourtRoomMapping> courtRoom3 = courtRoom("A");

        final LocalDate startDate = LocalDate.parse("2019-12-16");
        final String pEndDate = StringUtils.isNotBlank(endDate) ? endDate : StringUtils.EMPTY;

        when(commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId)).thenReturn(courtSites);
        when(commonXhibitReferenceDataService.getCourtRoom(eq(courtCentreId), eq(COURT_SITE_A_COURT_ROOM_ID))).thenReturn(courtRoom1);
        when(commonXhibitReferenceDataService.getCourtRoom(eq(courtCentreId), eq(COURT_SITE_B_COURT_ROOM_ID))).thenReturn(courtRoom2);
        when(commonXhibitReferenceDataService.getCourtRoom(eq(courtCentreId), eq(COURT_SITE_A_COURT_ROOM_ID_3))).thenReturn(courtRoom3);
        when(commonXhibitReferenceDataService.getCourtRoom(eq(courtCentreId), eq(UNKNOWN_COURT_SITE_COURT_ROOM_ID))).thenReturn(Optional.empty());
        when(commonXhibitReferenceDataService.getDefaultCrestCourtSiteCode(courtCentreId)).thenReturn("A");

        final JsonObject generatedCourtList = rangeSearchConverter.generateCourtListQueryPayload(courtCentreId, rangeSearchResponse, startDate, pEndDate);

        final JsonObject expectedCourtList = getJsonFile(expectedCourtListFilename);

        assertThat(generatedCourtList, is(notNullValue()));
        compareJson(generatedCourtList, expectedCourtList);
    }

    private Optional<CourtRoomMapping> courtRoom(final String crestCourtSiteCode) {
        return Optional.of(new CourtRoomMapping.Builder().withCrestCourtSiteCode(crestCourtSiteCode).build());
    }

    private JsonObject buildCourtSite(final String crestCourtSiteCode) {
        return Json.createObjectBuilder()
                .add("crestCourtSiteId", "001")
                .add("crestCourtSiteCode", crestCourtSiteCode)
                .add("crestCourtSiteName", "SITENAME " + crestCourtSiteCode)
                .add("courtType", "CROWN_COURT")
                .build();
    }
}

package uk.gov.moj.cpp.listing.common.xhibit;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.common.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingTypesList;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnitList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataCacheTest {

    @Mock
    private ReferenceDataLoader referenceDataLoader;

    @InjectMocks
    private ReferenceDataCache referenceDataCache;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    private static Optional<HearingTypesList> hearingTypesList;
    private static Optional<CourtMappingsList> courtMappingsList;

    private static final String courtId = "432";
    private static final UUID hearingTypeId = randomUUID();

    private static final String VENUE_NAME_FIELD = "venueName";
    private static final String COURT_ROOM_ID_FIELD = "courtroomId";
    private static final String COURT_ROOM_NAME_FIELD = "courtroomName";

    @Before
    public void setUp() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        initializeTestData();

        when(referenceDataLoader.getHearingTypesList()).thenReturn(hearingTypesList);
        when(referenceDataLoader.getXhibitCourtMappings()).thenReturn(courtMappingsList);

        referenceDataCache.initReferenceData();
    }

    @Test
    public void shouldPopulateOrganisationUnitsCache() {
        final UUID courtCentreId = randomUUID();
        final String ouCode = "C01BL00";

        final OrganisationUnit organisationUnit = new OrganisationUnit.Builder()
                .withId(courtCentreId)
                .withOucode(ouCode)
                .build();

        final OrganisationUnitList expectedOrganisationUnitList = new OrganisationUnitList(Arrays.asList(organisationUnit));

        when(referenceDataLoader.getOrganisationUnitListByOuCode(ouCode)).thenReturn(Optional.of(expectedOrganisationUnitList));

        final OrganisationUnitList actualOrganisationUnitList = referenceDataCache.getOrganisationUnitMapCache(ouCode);

        assertThat(actualOrganisationUnitList.getOrganisationunits().size(), is(expectedOrganisationUnitList.getOrganisationunits().size()));
        assertThat(actualOrganisationUnitList.getOrganisationunits().get(0).getOucode(), is(expectedOrganisationUnitList.getOrganisationunits().get(0).getOucode()));
        assertThat(actualOrganisationUnitList.getOrganisationunits().get(0).getId(), is(expectedOrganisationUnitList.getOrganisationunits().get(0).getId()));
    }

    @Test
    public void shouldPopulateCourtMappingsCache() {
        final CourtMapping expectedCourtMapping = getCourtMapping();
        assertThat(referenceDataCache.getCourtMappingsCache(courtId).get(0).getCrestCourtSiteCode(), is(expectedCourtMapping.getCrestCourtSiteCode()));
    }

    @Test
    public void shouldPopulateCourtRoomMappingsCache() {
        final UUID courtCentreId = randomUUID();

        final JsonObject courtRoomMappingJson = givenPayload("/mock-data/referencedata.query.cp-xhibit-courtroom-mappings.json");
        final Optional<CourtRoomMappingsList> expectedCourtRoomMappingsList =  Optional.of(jsonObjectConverter.convert(courtRoomMappingJson, CourtRoomMappingsList.class));

        when(referenceDataLoader.getCourtRoomMappingsList(courtCentreId)).thenReturn(expectedCourtRoomMappingsList);

        final CourtRoomMappingsList actualCourtRoomMappingsList = referenceDataCache.getCourtRoomMappingsMapCache(courtCentreId);

        assertThat(actualCourtRoomMappingsList.getCpXhibitCourtRoomMappings().size(), is(expectedCourtRoomMappingsList.get().getCpXhibitCourtRoomMappings().size()));
        assertThat(actualCourtRoomMappingsList.getCpXhibitCourtRoomMappings().get(0).getCourtRoomId(), is(expectedCourtRoomMappingsList.get().getCpXhibitCourtRoomMappings().get(0).getCourtRoomId()));
    }

    @Test
    public void shouldPopulateHearingTypesCache() {
        final HearingType expectedHearingType = getHearingType();
        assertThat(referenceDataCache.getHearingTypeCache(hearingTypeId).get().getId(), is(expectedHearingType.getId()));
    }

    @Test
    public void shouldPopulateJudiciariesCache() {
        final UUID judiciaryId = randomUUID();

        final String titlePrefix = "Mr";
        final String titleJudicialPrefix = "Recorder";

        final Judiciary judiciary = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitlePrefix(titlePrefix)
                .withTitleJudicialPrefix(titleJudicialPrefix)
                .build();

        final Optional<Judiciary> expectedJudiciary = Optional.of(judiciary);

        when(referenceDataLoader.getJudiciary(judiciaryId)).thenReturn(expectedJudiciary);


        final Optional<Judiciary> actualJudiciary = referenceDataCache.getJudiciariesMapCache(judiciaryId);

        assertThat(actualJudiciary.isPresent(), is(true));
        assertThat(actualJudiciary.get().getTitlePrefix(), is(expectedJudiciary.get().getTitlePrefix()));
        assertThat(actualJudiciary.get().getTitleJudicialPrefix(), is(expectedJudiciary.get().getTitleJudicialPrefix()));
    }

    @Test
    public void shouldPopulateCpCourtRoomCache() {
        final UUID courtCentreId = randomUUID();
        final JsonObject courtRooms = getPayloadForCourtRooms(courtCentreId.toString());
        final List<JsonObject> courtRoomList = courtRooms.getJsonArray("courtrooms").getValuesAs(JsonObject.class);

        when(referenceDataLoader.getCpCourtRoom(courtCentreId)).thenReturn(courtRoomList);

        final List<JsonObject> actualCpCourtRoom = referenceDataCache.getCpCourtRoomCache(courtCentreId);

        assertThat(courtRoomList.size(), is(actualCpCourtRoom.size()));
        assertEquals(courtRoomList.get(0).get("id"), actualCpCourtRoom.get(0).get("id"));
        assertEquals(courtRoomList.get(0).get(VENUE_NAME_FIELD), actualCpCourtRoom.get(0).get(VENUE_NAME_FIELD));
        assertEquals(courtRoomList.get(0).get(COURT_ROOM_ID_FIELD), actualCpCourtRoom.get(0).get(COURT_ROOM_ID_FIELD));
        assertEquals(courtRoomList.get(0).get(COURT_ROOM_NAME_FIELD), actualCpCourtRoom.get(0).get(COURT_ROOM_NAME_FIELD));
    }

    private void initializeTestData() {
        hearingTypesListTestData();
        courtMappingsListTestData();
    }

    private void hearingTypesListTestData() {
        hearingTypesList = Optional.of(new HearingTypesList(Arrays.asList(getHearingType())));
    }

    private HearingType getHearingType() {
        return new HearingType.Builder()
                    .withId(hearingTypeId)
                    .withExhibitHearingCode("PTP")
                    .withExhibitHearingDescription("Plea & Trial Preparation")
                    .build();
    }

    private void courtMappingsListTestData() {
        courtMappingsList = Optional.of(new CourtMappingsList(Arrays.asList(getCourtMapping())));
    }

    private CourtMapping getCourtMapping() {
        final String ouCode = "OUCODE";
        final String courtSiteId = "433";
        final String crestCourtName = "BLACKFRIARS";
        final String courtSiteName = "BLACKFRIARS";
        final String courtShortName = "BLF";
        final String courtSiteCode = "B";
        final String courtType = "MAGISTRATE";

        return new CourtMapping.Builder()
                .withOucode(ouCode)
                .withCrestCourtId(courtId)
                .withCrestCourtSiteId(courtSiteId)
                .withCrestCourtName(crestCourtName)
                .withCrestCourtSiteName(courtSiteName)
                .withCrestCourtShortName(courtShortName)
                .withCrestCourtSiteCode(courtSiteCode)
                .withCourtType(courtType)
                .build();
    }

    private JsonObject getPayloadForCourtRooms(String id) {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("oucode", "B01LY00")
                .add("oucodeL3Name", "South Western (Lavender Hill)")
                .add("oucodeL3WelshName", "welshName_Test")
                .add("courtrooms", getCourtRooms())
                .build();
    }

    private JsonArray getCourtRooms() {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add(VENUE_NAME_FIELD, "BEXLEY MAGISTRATES' COURT")
                .add(COURT_ROOM_ID_FIELD, 12)
                .add(COURT_ROOM_NAME_FIELD, "Courtroom 01"))
                .build();
    }
}
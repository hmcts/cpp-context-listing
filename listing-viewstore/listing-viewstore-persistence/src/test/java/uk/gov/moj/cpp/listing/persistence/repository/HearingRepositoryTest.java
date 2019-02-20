package uk.gov.moj.cpp.listing.persistence.repository;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.time.LocalDate.now;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.ALL_AUTHORITY_CODES_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.EARLIEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.LATEST_SEARCH_DATE;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.utils.FileUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest extends BaseTransactionalTest {

    private static final Boolean UNALLOCATED = false;
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID OTHER_HEARING_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID OTHER_COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID AUTHORITY_ID = UUID.randomUUID();
    private static final UUID OTHER_AUTHORITY_ID = UUID.randomUUID();
    private static final String AUTHORITY_CODE_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final Type HEARING_TYPE = Type.type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
    private static final Type OTHER_HEARING_TYPE = Type.type().withId(UUID.randomUUID()).withDescription("SENTENCE").build();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final String JUDICIAL_ID = "0ab98bfb-fc34-44c4-a573-3801343cf123";
    private static final String OTHER_JUDICIAL_ID = "a666923b-bbc1-4ed7-b340-c72f9341035b";
    private static final JurisdictionType OTHER_JURISDICTION_TYPE = JurisdictionType.MAGISTRATES;
    private static final LocalDate START_SEARCH_DATE = now();
    private static final LocalDate END_SEARCH_DATE = now().plusDays(1);
    private static final LocalDate START_DATE = now();
    private static final LocalDate END_DATE = now().plusDays(2);
    private static final LocalDateTime START_TIME = LocalDateTime.now();
    private static final LocalDateTime END_TIME = LocalDateTime.now().plusHours(2);
    private static final LocalDateTime EARLIEST_SEARCH_DATE_TIME = LocalDateTime.of(START_SEARCH_DATE, LocalTime.MIN);
    private static final LocalDateTime LATEST_SEARCH_DATE_TIME = LocalDateTime.of(START_SEARCH_DATE, LocalTime.MAX);
    private static final LocalDate HEARING_DATE = now();

    private static final Boolean ALLOCATED = BOOLEAN.next();
    private static final String HEARING_ID_FIELD = "HEARING_ID_FIELD";
    private static final String JURISDICTION_TYPE_FIELD = "JURISDICTION_TYPE_FIELD";
    private static final String JUDICIAL_ID_FIELD = "JUDICIAL_ID";
    private static final String ALLOCATED_FIELD = "ALLOCATED_FIELD";
    private static final String COURT_CENTRE_ID_FIELD = "COURT_CENTRE_ID_FIELD";
    private static final String COURT_ROOM_ID_FIELD = "COURT_ROOM_ID_FIELD";
    private static final String AUTHORITY_ID_FIELD = "AUTHORITY_ID_FIELD";
    private static final String HEARING_TYPE_DESCRIPTION_FIELD = "HEARING_TYPE_DESCRIPTION_FIELD";
    private static final String HEARING_TYPE_ID_FIELD = "HEARING_TYPE_ID_FIELD";
    private static final String START_DATE_FIELD = "START_DATE_FIELD";
    private static final String END_DATE_FIELD = "END_DATE_FIELD";
    private static final String START_TIME_FIELD = "START_TIME_FIELD";
    private static final String END_TIME_FIELD = "END_TIME_FIELD";
    private static final String HEARING_DATE_FIELD = "HEARING_DATE_FIELD";
    private static final String CASE_REFERENCE = "45DI277164";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Test
    public void shouldFindHearingById() {
        final Hearing actualHearing = saveHearingJson(
                UUID.randomUUID(),
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                UNALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE);
        final Hearing expectedHearing = hearingRepository.findBy(actualHearing.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedHearing, actualHearing));
    }

    @Test
    public void shouldReturnEmptyHearingsWhereQueryDoesNotFindResults() {
        givenHearings();

        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                UUID.randomUUID().toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldSaveAndFindHearingWhereQueryFindsResultsJson() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalCourtCentre() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                null,
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalCourtRoom() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalAuthorityCode() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                HearingRepository.ALL_AUTHORITY_CODES_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalHearingType() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                null,
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithManyOptionalParameters() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                null,
                ALL_AUTHORITY_CODES_SEARCH,
                null,
                null,
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithManyOptionalParameters2() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                null,
                ALL_AUTHORITY_CODES_SEARCH,
                null,
                null,
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalJurisdictionType() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                null,
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalSearchDate() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithSpecificSearchDateAndAllTimes() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalStartTime() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                END_TIME.toString());

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalEndTime() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                START_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndNotFindHearingJsonWithStartTimeMatchingAndEndTimeNotMatching() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                START_TIME.minusHours(5).toString(),
                END_TIME.minusHours(5).toString());

        //then
        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldSaveAndNotFindHearingJsonWithSearchDateNotMatching() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE.plusDays(10)),
                to(END_SEARCH_DATE.plusDays(10)));

        //then
        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldSaveAndNotFindHearingJsonWithHearingStartDateBetweenSearchDates() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE.minusDays(1)),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndNotFindHearingJsonWithHearingEndDateBetweenSearchDates() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE.plusDays(1)),
                to(END_SEARCH_DATE.plusDays(2)));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndNotFindHearingJsonWithHearingStartDateAndHearingEndDateSpanningOverTheSearchDates() {

        //given
        givenHearings();


        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE.plusDays(1)),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }


    @Test
    public void shouldSaveAndFindHearingForPublicCourList() {

        //given
        givenHearings();

        //when
        final Hearing actualHearings = hearingRepository.findHearingsForPublicStandardList(
                UNALLOCATED    ,
                COURT_CENTRE_ID.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary.size()", equalTo(2)));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary[0].judicialId", equalTo(JUDICIAL_ID)));

        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings.size()", equalTo(1)));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId[0].hearingDate", equalTo(START_DATE.toString())));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId[0].hearingsByHearingDate[0].hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())));
    }

    @Test
    public void shouldSaveAndfindAlphabeticalCourtList(){
        givenHearings();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(UNALLOCATED, COURT_CENTRE_ID.toString(),to(HEARING_DATE));
        assertThat(foundHearings.size(), is(1));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$.size()", equalTo(1)));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingDate", equalTo(to(HEARING_DATE))));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].defendants[0].lastName", equalTo("JAMES")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].defendants[0].firstName", equalTo("Lina")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].courtRoomId", equalTo(COURT_ROOM_ID.toString())));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].defendants[1].lastName", equalTo("PALMER")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].defendants[1].firstName", equalTo("Virgie")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].startTime", equalTo(START_TIME.toString())));
    }

    @Test
    public void shouldSavebutNotFindAlphabeticalCourtList(){
        givenHearings();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(ALLOCATED, COURT_CENTRE_ID.toString(),"1900-01-01");
        assertThat(foundHearings.get(0).getProperties(), nullValue());
    }

    private void givenHearings() {
        saveHearingJson(
                HEARING_ID,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                UNALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE);
        saveHearingJson(
                OTHER_HEARING_ID,
                OTHER_COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                OTHER_AUTHORITY_ID,
                OTHER_HEARING_TYPE,
                OTHER_JURISDICTION_TYPE,
                OTHER_JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE);
    }

    private Hearing saveHearingJson(final UUID hearingId,
                                    final UUID courtCentreId,
                                    final UUID courtRoomId,
                                    final boolean allocated,
                                    final UUID authorityId,
                                    final Type hearingType,
                                    final JurisdictionType jurisdictionType,
                                    final String judicialId,
                                    final LocalDate startDate,
                                    final LocalDate endDate,
                                    final LocalDateTime startTime,
                                    final LocalDateTime endTime,
                                    final LocalDate hearingDate) {
        final Hearing hearing = createHearingJson(
                hearingId,
                courtCentreId,
                courtRoomId,
                allocated,
                authorityId,
                hearingType,
                jurisdictionType,
                judicialId,
                startDate,
                endDate,
                startTime,
                endTime,
                hearingDate);

        hearingRepository.save(hearing);

        return hearing;
    }

    private Hearing createHearingJson(final UUID hearingId,
                                      final UUID courtCentreId,
                                      final UUID courtRoomId,
                                      final boolean allocated,
                                      final UUID authorityId,
                                      final Type hearingType,
                                      final JurisdictionType jurisdictionType,
                                      final String judicialId,
                                      final LocalDate startDate,
                                      final LocalDate endDate,
                                      final LocalDateTime startTime,
                                      final LocalDateTime endTime,
                                      final LocalDate hearingDate) {
        final String hearingString = createHearingString(
                hearingId,
                courtCentreId,
                courtRoomId,
                allocated,
                authorityId,
                hearingType,
                jurisdictionType,
                judicialId,
                startDate,
                endDate,
                startTime,
                endTime,
                hearingDate);
         return new HearingBuilder()
                .setId(hearingId)
                .setProperties(JacksonUtil.toJsonNode(hearingString))
                .build();
    }

    private String createHearingString(final UUID hearingId,
                                       final UUID courtCentreId,
                                       final UUID courtRoomId,
                                       final boolean allocated,
                                       final UUID authorityId,
                                       final Type hearingType,
                                       final JurisdictionType jurisdictionType,
                                       final String judicialId,
                                       final LocalDate startDate,
                                       final LocalDate endDate,
                                       final LocalDateTime startTime,
                                       final LocalDateTime endTime,
                                       final LocalDate hearingDate) {

        String hearingString = FileUtil.getPayload("test-data/sample-hearing.json");
        return hearingString
                .replaceAll(HEARING_ID_FIELD, hearingId.toString())
                .replaceAll(JURISDICTION_TYPE_FIELD, jurisdictionType.toString())
                .replaceAll(JUDICIAL_ID_FIELD, judicialId)
                .replaceAll(ALLOCATED_FIELD, Boolean.toString(allocated))
                .replaceAll(COURT_CENTRE_ID_FIELD, courtCentreId.toString())
                .replaceAll(COURT_ROOM_ID_FIELD, courtRoomId.toString())
                .replaceAll(AUTHORITY_ID_FIELD, authorityId.toString())
                .replaceAll(HEARING_TYPE_ID_FIELD, hearingType.getId().toString())
                .replaceAll(HEARING_TYPE_DESCRIPTION_FIELD, hearingType.getDescription())
                .replaceAll(START_DATE_FIELD, to(startDate))
                .replaceAll(END_DATE_FIELD, to(endDate))
                .replaceAll(START_TIME_FIELD, startTime.toString())
                .replaceAll(END_TIME_FIELD, endTime.toString())
                .replaceAll(HEARING_DATE_FIELD, to(hearingDate));

    }
}

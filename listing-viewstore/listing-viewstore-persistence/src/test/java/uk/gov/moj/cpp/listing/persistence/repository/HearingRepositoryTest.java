package uk.gov.moj.cpp.listing.persistence.repository;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.time.LocalDate.now;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.domain.Type.type;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


// It seems that this must only be run with PersistenceTestSuite.
@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest extends BaseTransactionalTest {

    private static final Boolean UNALLOCATED = false;
    private static final String UNALLOCATEDSTR = "false";
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID OTHER_HEARING_ID = UUID.randomUUID();
    private static final UUID OTHER_HEARING_ID2 = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID OTHER_COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID AUTHORITY_ID = UUID.randomUUID();
    private static final UUID OTHER_AUTHORITY_ID = UUID.randomUUID();
    private static final String AUTHORITY_CODE_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final Type HEARING_TYPE = type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
    private static final Type OTHER_HEARING_TYPE = type().withId(UUID.randomUUID()).withDescription("SENTENCE").build();
    private static final JurisdictionType JURISDICTION_TYPE = CROWN;
    private static final String JUDICIAL_ID = "0ab98bfb-fc34-44c4-a573-3801343cf123";
    private static final String OTHER_JUDICIAL_ID = "a666923b-bbc1-4ed7-b340-c72f9341035b";
    private static final JurisdictionType OTHER_JURISDICTION_TYPE = MAGISTRATES;
    private static final LocalDate START_SEARCH_DATE = now();
    private static final LocalDate END_SEARCH_DATE = now().plusDays(1);
    private static final LocalDate WEEK_COMMENCING_START = now();
    private static final LocalDate WEEK_COMMENCING_END = now().plusDays(8);
    private static final LocalDate START_DATE = now();
    private static final LocalDate END_DATE = now().plusDays(2);
    private static final LocalDate WEEK_COMMENCING_START_DATE = now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = now().plusDays(7);
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
    private static final String WEEK_COMMENCING_START_FIELD = "WEEK_COMMENCING_START_FIELD";
    private static final String WEEK_COMMENCING_END_FIELD = "WEEK_COMMENCING_END_FIELD";
    private static final String START_TIME_FIELD = "START_TIME_FIELD";
    private static final String END_TIME_FIELD = "END_TIME_FIELD";
    private static final String HEARING_DATE_FIELD = "HEARING_DATE_FIELD";
    private static final String CASE_REFERENCE = "45DI277164";
    private static final String MASTER_DEFENDANT_ID = "e2b13dc1-de95-11e8-9df5-e56feb0784f6";
    private static final String LINKED_CASE_URN = "45DI277164";
    private static final String EMPTY_STRING = "";
    private static final String TEST_DATA_SAMPLE_HEARING_JSON = "test-data/sample-hearing.json";
    private static final String TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON = "test-data/sample-unscheduled-hearing.json";
    private static final String TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON = "test-data/sample-unscheduled-hearing-without-case.json";

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
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
        final Hearing expectedHearing = hearingRepository.findBy(actualHearing.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedHearing, actualHearing));
    }

    @Test
    public void shouldReturnEmptyHearingsWhereQueryDoesNotFindResults() {
        givenHearings();

        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATEDSTR,
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
                UNALLOCATEDSTR,
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
    public void shouldSaveAndFindHearingWhereQueryFindsResultsByWeekCommencingDateRange() {

        //given hearing with fixed date
        givenHearings();

        //given hearing with commencing date
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final Type hearingType = type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
        final String judicialId = UUID.randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_CODE_SEARCH,
                null,
                CROWN.toString(),
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));

    }

    @Test
    public void shouldSaveAndFindUnallocatedHearingWhereQueryFindsResultsByWeekCommencingDateRange() {

        //given hearing with fixed date
        givenHearings();

        //given hearing with commencing date
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final Type hearingType = type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
        final String judicialId = UUID.randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId);

        //when
        final List<Hearing> actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_CODE_SEARCH,
                null,
                CROWN.toString(),
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE,
                false);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));

    }

    @Test
    public void shouldSaveAndFindHearingWhereQueryFindsResultsByWeekCommencingDateRangeAndAuthorityIdNotSpecified() {

        //given hearing with fixed date
        givenHearings();

        //given hearing with commencing date
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final Type hearingType = type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
        final String judicialId = UUID.randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                null,
                null,
                HearingRepository.ALL_AUTHORITY_CODES_SEARCH,
                null,
                null,
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        //then
        assertThat(actualHearings.size(), is(3));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(OTHER_COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(OTHER_AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(OTHER_JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));


    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalCourtCentre() {

        //given
        givenHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATEDSTR,
                "null",
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
                UNALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                "null",
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
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATEDSTR,
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
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                "null",
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
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                "null",
                ALL_AUTHORITY_CODES_SEARCH,
                "null",
                "null",
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
                UNALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                "null",
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
                UNALLOCATEDSTR,
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
                UNALLOCATEDSTR,
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
                UNALLOCATEDSTR,
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
                UNALLOCATEDSTR,
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
                UNALLOCATEDSTR,
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
                UNALLOCATED,
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
    public void shouldSaveAndFindAlphabeticalCourtList() {
        givenHearings();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(UNALLOCATED, COURT_CENTRE_ID.toString(), to(HEARING_DATE));

        assertThat(foundHearings.size(), is(1));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$.size()", equalTo(1)));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingDate", equalTo(to(HEARING_DATE))));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].defendants[0].lastName", equalTo("JAMES")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].defendants[0].firstName", equalTo("Lina")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].defendants[1].lastName", equalTo("PALMER")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].defendants[1].firstName", equalTo("Virgie")));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.hearingDays[0].startTime", equalTo(START_TIME.toString())));
    }

    @Test
    public void shouldSavebutNotFindAlphabeticalCourtList() {
        givenHearings();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(ALLOCATED, COURT_CENTRE_ID.toString(), "1900-01-01");
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
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);

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
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
    }

    public void givenHearingsWithWeekCommencing(final UUID hearingId,
                                                final UUID courtCentreId,
                                                final UUID authorityId,
                                                final Type hearingType,
                                                final JurisdictionType jurisdictionType,
                                                final String judicialId) {
        saveHearingJson(
                hearingId,
                courtCentreId,
                null,
                UNALLOCATED,
                authorityId,
                hearingType,
                jurisdictionType,
                judicialId,
                null,
                null,
                null,
                null,
                null,
                WEEK_COMMENCING_START,
                WEEK_COMMENCING_END,
                "test-data/sample-hearing-for-week-commencing.json"
        );
    }

    @Test
    public void shouldSaveAndFindAvailableHearingByCaseUrn() {
        //given
        givenAvailableHearings();
        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(CASE_REFERENCE);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(EMPTY_STRING);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(EMPTY_STRING);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                HEARING_ID.toString(),
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", not(HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));
    }

    @Test
    public void shouldSaveAndFindAvailableHearingByHearingIdAndMasterDefendantId() {
        //given
        givenAvailableHearings();
        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(EMPTY_STRING);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(MASTER_DEFENDANT_ID);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(EMPTY_STRING);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                HEARING_ID.toString(),
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", not(HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].defendants[0].masterDefendantId", equalTo(MASTER_DEFENDANT_ID)));
    }

    @Test
    public void shouldSaveAndFindAvailableHearingByCaseUrnForLinkedCases() {
        //given
        givenAvailableHearings();

        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(EMPTY_STRING);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(EMPTY_STRING);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(CROWN.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(EMPTY_STRING);

        final String caseUrnForLinkedCases = "45DI277164";

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                null,
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                caseUrnForLinkedCases);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", not(OTHER_HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].defendants[0].masterDefendantId", equalTo(MASTER_DEFENDANT_ID)));
    }

    @Test
    public void shouldSaveAndFindAvailableHearingByMasterDefendantId() {
        //given
        givenAvailableHearings();
        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(EMPTY_STRING);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(MASTER_DEFENDANT_ID);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(EMPTY_STRING);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                null,
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", not(OTHER_HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].defendants[0].masterDefendantId", equalTo(MASTER_DEFENDANT_ID)));
    }

    @Test
    public void shouldSaveAndFindAvailableHearingByCaseUrnForCrownAndMags() {
        //given
        givenAvailableHearingsForCrownAndMags();
        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(CASE_REFERENCE);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(EMPTY_STRING);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());
        jurisdictionTypeSet.add(JurisdictionType.MAGISTRATES.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(EMPTY_STRING);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                HEARING_ID.toString(),
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JurisdictionType.CROWN.name())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));

        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID2.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JurisdictionType.MAGISTRATES.name())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));

    }

    @Test
    public void shouldSaveAndFindAvailableHearingByLinkedCase() {
        //given
        givenAvailableHearings();
        final Set<String> caseUrnSet = new HashSet<>();
        caseUrnSet.add(EMPTY_STRING);

        final Set<String> masterDefendantIdSet = new HashSet<>();
        masterDefendantIdSet.add(EMPTY_STRING);

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        final Set<String> linkedCaseUrn = new HashSet<>();
        linkedCaseUrn.add(LINKED_CASE_URN);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                HEARING_ID.toString(),
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", not(HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].linkedCases[0].caseUrn", equalTo(LINKED_CASE_URN)));
    }


    private void givenAvailableHearings() {
        saveHearingJson(
                HEARING_ID,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
        saveHearingJson(
                OTHER_HEARING_ID,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
    }

    private void givenAvailableHearingsForCrownAndMags() {
        saveHearingJson(
                HEARING_ID,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
        saveHearingJson(
                OTHER_HEARING_ID,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
        saveHearingJson(
                OTHER_HEARING_ID2,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                ALLOCATED,
                AUTHORITY_ID,
                HEARING_TYPE,
                OTHER_JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);

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
                                    final LocalDate hearingDate,
                                    final LocalDate weekCommencingStartDate,
                                    final LocalDate weekCommencingEndDate,
                                    final String fileLocation) {
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
                hearingDate,
                weekCommencingStartDate,
                weekCommencingEndDate,
                fileLocation
        );

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
                                      final LocalDate hearingDate,
                                      final LocalDate weekCommencingStartDate,
                                      final LocalDate weekCommencingEndDate,
                                      final String fileLocation) {
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
                hearingDate,
                weekCommencingStartDate,
                weekCommencingEndDate,
                fileLocation);
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
                                       final LocalDate hearingDate,
                                       final LocalDate weekCommencingStartDate,
                                       final LocalDate weekCommencingEndDate,
                                       final String fileLocation) {

        String hearingString = FileUtil.getPayload(fileLocation);
        final String updatedHearingString = hearingString
                .replaceAll(HEARING_ID_FIELD, hearingId.toString())
                .replaceAll(JURISDICTION_TYPE_FIELD, jurisdictionType.toString())
                .replaceAll(JUDICIAL_ID_FIELD, judicialId);
        return weekCommencingStartDate == null ?
                updatedHearingString
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
                        .replaceAll(HEARING_DATE_FIELD, to(hearingDate))
                :
                updatedHearingString
                        .replaceAll(ALLOCATED_FIELD, Boolean.toString(allocated))
                        .replaceAll(COURT_CENTRE_ID_FIELD, courtCentreId.toString())
                        .replaceAll(AUTHORITY_ID_FIELD, authorityId.toString())
                        .replaceAll(HEARING_TYPE_ID_FIELD, hearingType.getId().toString())
                        .replaceAll(HEARING_TYPE_DESCRIPTION_FIELD, hearingType.getDescription())
                        .replaceAll(WEEK_COMMENCING_START_FIELD, to(weekCommencingStartDate))
                        .replaceAll(WEEK_COMMENCING_END_FIELD, to(weekCommencingEndDate));

    }

    @Test
    public void shouldFindUnscheduledHearingsWithoutParameters() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, null);

        //then
        assertThat(actualHearings.size(), is(3));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID.toString(), OTHER_HEARING_ID2.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", anyOf(equalTo(COURT_CENTRE_ID.toString()), equalTo(OTHER_COURT_CENTRE_ID.toString()))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", anyOf(equalTo(JURISDICTION_TYPE.toString()), equalTo(OTHER_JURISDICTION_TYPE.toString()))));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
    }

    @Test
    public void shouldFindUnscheduledHearingsWithCaseUrn() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("45DI277164", null);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", anyOf(equalTo(COURT_CENTRE_ID.toString()), equalTo(OTHER_COURT_CENTRE_ID.toString()))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", anyOf(equalTo(JURISDICTION_TYPE.toString()), equalTo(OTHER_JURISDICTION_TYPE.toString()))));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
    }
    @Test
    public void shouldFindUnscheduledHearingsWithApplicationCaseReference() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("TFL7328425-1", null);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(OTHER_HEARING_ID2.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldFindUnscheduledHearingsWithTypeOfListing() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, "0b1e1e98-a5b2-460a-a851-17dd6f47c1a7");

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", anyOf(equalTo(COURT_CENTRE_ID.toString()), equalTo(OTHER_COURT_CENTRE_ID.toString()))));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", anyOf(equalTo(JURISDICTION_TYPE.toString()), equalTo(OTHER_JURISDICTION_TYPE.toString()))));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
    }

    @Test
    public void shouldFindUnscheduledHearingsWithCourtCentreIds() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, null, Collections.singleton(COURT_CENTRE_ID.toString()));

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID2.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    public List<String> extractFields(final List<Hearing> actualHearings, final String fieldPath) {
        final JsonPath jsonPath = JsonPath.compile(fieldPath, new Predicate[0]);
        return actualHearings.stream()
                .map(hearing -> jsonPath.read(hearing.getProperties().toString()))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Test
    public void shouldFindUnscheduledHearingsWithAllFields() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("45DI277164", "0b1e1e98-a5b2-460a-a851-17dd6f47c1a7", Collections.singleton(COURT_CENTRE_ID.toString()));

        //then
        assertThat(actualHearings.size(), is(1));

        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString()));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }


    private void givenUnscheduledHearings() {
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
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON);

        saveHearingJson(
                OTHER_HEARING_ID,
                OTHER_COURT_CENTRE_ID,
                COURT_ROOM_ID,
                true,
                OTHER_AUTHORITY_ID,
                OTHER_HEARING_TYPE,
                OTHER_JURISDICTION_TYPE,
                OTHER_JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON);

        saveHearingJson(
                OTHER_HEARING_ID2,
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
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON);

        saveHearingJson(
                UUID.randomUUID(),
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                false,
                AUTHORITY_ID,
                HEARING_TYPE,
                JURISDICTION_TYPE,
                JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);

        saveHearingJson(
                UUID.randomUUID(),
                OTHER_COURT_CENTRE_ID,
                COURT_ROOM_ID,
                false,
                OTHER_AUTHORITY_ID,
                OTHER_HEARING_TYPE,
                OTHER_JURISDICTION_TYPE,
                OTHER_JUDICIAL_ID,
                START_DATE,
                END_DATE,
                START_TIME,
                END_TIME,
                HEARING_DATE,
                null,
                null,
                TEST_DATA_SAMPLE_HEARING_JSON);
    }


}

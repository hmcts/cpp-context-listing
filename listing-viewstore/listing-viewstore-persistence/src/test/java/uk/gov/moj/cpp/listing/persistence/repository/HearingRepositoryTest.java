package uk.gov.moj.cpp.listing.persistence.repository;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static com.vladmihalcea.hibernate.type.json.internal.JacksonUtil.toJsonNode;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.domain.Type.type;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.ALL_AUTHORITY_CODES_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.EARLIEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.LATEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.persistence.repository.utils.HearingRepositoryContext.hearingRepositoryContext;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.utils.HearingRepositoryContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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

    private static final Boolean ALLOCATED = TRUE;
    private static final Boolean UNALLOCATED = FALSE;
    private static final Boolean RANDOM_ALLOCATED = BOOLEAN.next();
    private static final Boolean NOT_VACATED = FALSE;
    private static final String UNALLOCATED_STR = "false";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID OTHER_HEARING_ID = randomUUID();
    private static final UUID OTHER_HEARING_ID2 = randomUUID();
    private static final UUID VACATED_HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID OTHER_COURT_CENTRE_ID = randomUUID();
    private static final UUID AUTHORITY_ID = randomUUID();
    private static final UUID OTHER_AUTHORITY_ID = randomUUID();
    private static final String AUTHORITY_CODE_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final Type HEARING_TYPE = type().withId(randomUUID()).withDescription("TRIAL").build();
    private static final Type OTHER_HEARING_TYPE = type().withId(randomUUID()).withDescription("SENTENCE").build();
    private static final JurisdictionType JURISDICTION_TYPE = CROWN;
    private static final String JUDICIAL_ID = "0ab98bfb-fc34-44c4-a573-3801343cf123";
    private static final String OTHER_JUDICIAL_ID = "a666923b-bbc1-4ed7-b340-c72f9341035b";
    private static final JurisdictionType OTHER_JURISDICTION_TYPE = MAGISTRATES;
    private static final LocalDate START_SEARCH_DATE = now();
    private static final LocalDate END_SEARCH_DATE = now().plusDays(1);
    private static final LocalDate START_DATE = now();
    private static final LocalDate END_DATE = now().plusDays(2);
    private static final LocalDate WEEK_COMMENCING_START_DATE = now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = now().plusDays(7);
    private static final LocalDateTime START_TIME = LocalDateTime.now();
    private static final LocalDateTime END_TIME = LocalDateTime.now().plusHours(2);
    private static final LocalDateTime EARLIEST_SEARCH_DATE_TIME = LocalDateTime.of(START_SEARCH_DATE, LocalTime.MIN);
    private static final LocalDateTime LATEST_SEARCH_DATE_TIME = LocalDateTime.of(START_SEARCH_DATE, LocalTime.MAX);
    private static final LocalDate HEARING_DATE = now();
    private static final LocalDate DAY_1_HEARING_DATE = now();
    private static final LocalDate DAY_2_HEARING_DATE = now().plusDays(1);
    private static final LocalDate DAY_3_HEARING_DATE = now().plusDays(2);
    private static final LocalDateTime DAY_1_START_TIME = LocalDateTime.now();
    private static final LocalDateTime DAY_1_END_TIME = DAY_1_START_TIME.plusHours(2);
    private static final LocalDateTime DAY_2_START_TIME = DAY_1_START_TIME.plusDays(1);
    private static final LocalDateTime DAY_2_END_TIME = DAY_2_START_TIME.plusHours(2);
    private static final LocalDateTime DAY_3_START_TIME = DAY_1_START_TIME.plusDays(2);
    private static final LocalDateTime DAY_3_END_TIME = DAY_3_START_TIME.plusHours(2);

    private static final String HEARING_ID_FIELD = "HEARING_ID_FIELD";
    private static final String JURISDICTION_TYPE_FIELD = "JURISDICTION_TYPE_FIELD";
    private static final String JUDICIAL_ID_FIELD = "JUDICIAL_ID";
    private static final String ALLOCATED_FIELD = "ALLOCATED_FIELD";
    private static final String VACATED_TRIAL_FIELD = "VACATED_TRIAL_FIELD";
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
    private static final String DAY_1_HEARING_DATE_FIELD = "DAY_1_HEARING_DATE_FIELD";
    private static final String DAY_2_HEARING_DATE_FIELD = "DAY_2_HEARING_DATE_FIELD";
    private static final String DAY_3_HEARING_DATE_FIELD = "DAY_3_HEARING_DATE_FIELD";
    private static final String DAY_1_START_TIME_FIELD = "DAY_1_START_TIME_FIELD";
    private static final String DAY_1_END_TIME_FIELD = "DAY_1_END_TIME_FIELD";
    private static final String DAY_2_START_TIME_FIELD = "DAY_2_START_TIME_FIELD";
    private static final String DAY_2_END_TIME_FIELD = "DAY_2_END_TIME_FIELD";
    private static final String DAY_3_START_TIME_FIELD = "DAY_3_START_TIME_FIELD";
    private static final String DAY_3_END_TIME_FIELD = "DAY_3_END_TIME_FIELD";
    private static final String DAY_1_IS_CANCELLED_FIELD = "DAY_1_IS_CANCELLED_FIELD";
    private static final String DAY_2_IS_CANCELLED_FIELD = "DAY_2_IS_CANCELLED_FIELD";
    private static final String DAY_3_IS_CANCELLED_FIELD = "DAY_3_IS_CANCELLED_FIELD";

    private static final String TEST_DATA_SAMPLE_HEARING_JSON = "test-data/sample-hearing.json";
    private static final String TEST_DATA_SAMPLE_HEARING_NULL_VACATED_JSON = "test-data/sample-hearing-null-vacated.json";
    private static final String TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON = "test-data/sample-unscheduled-hearing.json";
    private static final String TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON = "test-data/sample-unscheduled-hearing-without-case.json";
    private static final String TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON = "test-data/sample-multiday-hearing.json";
    private static final String SAMPLE_UNALLOCATED_HEARING_FOR_WEEK_COMMENCING = "test-data/sample-unallocated-hearing-for-week-commencing.json";

    @Inject
    private HearingRepository hearingRepository;

    @Test
    public void shouldFindHearingById() {
        final Hearing actualHearing = givenHearingsExist().get(0);

        final Hearing expectedHearing = hearingRepository.findBy(actualHearing.getId());

        assertTrue(reflectionEquals(expectedHearing, actualHearing));
    }

    @Test
    public void shouldReturnEmptyHearingsWhereQueryDoesNotFindResults() {
        givenHearingsExist();

        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
                randomUUID().toString(),
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
    public void shouldNotRetrieveVacatedHearingWhenVacatedTrueAndFindHearingsInvoked() {
        //given
        givenHearingsWithVacated(TRUE);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                RANDOM_ALLOCATED.toString(),
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveVacatedHearingWhenVacatedNullOrFalseAndFindHearingsInvoked() {
        //given
        givenHearingsWithVacated(null);

        //when
        List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findHearings(
                RANDOM_ALLOCATED.toString(),
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_CODE_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingWhereQueryFindsResultsByWeekCommencingDateRange() {
        //given hearing with fixed date
        givenHearingsExist();

        //given hearing with commencing date
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final Type hearingType = type().withId(randomUUID()).withDescription("TRIAL").build();
        final String judicialId = randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId, FALSE);

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
        givenHearingsExist();

        //given hearing with commencing date
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final Type hearingType = type().withId(randomUUID()).withDescription("TRIAL").build();
        final String judicialId = randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId, FALSE);

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
    public void shouldNotRetrieveVacatedHearingWhenVacatedIsTrueAndFindUnallocatedHearingsByWeekCommencingRangeInvoked() {
        givenHearingsWithWeekCommencing(HEARING_ID, COURT_CENTRE_ID, AUTHORITY_ID, HEARING_TYPE, CROWN, JUDICIAL_ID, TRUE);

        //when
        final List<Hearing> actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE,
                false);

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveHearingWhenVacatedIsNullOrFalseAndFindUnallocatedHearingsByWeekCommencingRangeInvoked() {
        givenHearingsWithWeekCommencing(HEARING_ID, COURT_CENTRE_ID, AUTHORITY_ID, HEARING_TYPE, CROWN, JUDICIAL_ID, FALSE);
        givenHearingsWithWeekCommencing(OTHER_HEARING_ID, OTHER_COURT_CENTRE_ID, AUTHORITY_ID, OTHER_HEARING_TYPE, CROWN, JUDICIAL_ID, null);

        List<Hearing> actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE,
                false);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                OTHER_COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE,
                false);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingWhereQueryFindsResultsByWeekCommencingDateRangeAndAuthorityIdNotSpecified() {
        //given hearing with fixed date
        givenHearingsExistWithDifferentStartAndEndDates();

        //given hearing with week commencing date
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final Type hearingType = type().withId(randomUUID()).withDescription("TRIAL").build();
        final String judicialId = randomUUID().toString();
        givenHearingsWithWeekCommencing(hearingId, courtCentreId, AUTHORITY_ID, hearingType, CROWN, judicialId, FALSE);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                null,
                null,
                ALL_AUTHORITY_CODES_SEARCH,
                null,
                null,
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        //then
        assertThat(actualHearings.size(), is(3));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(OTHER_COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(OTHER_AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(OTHER_JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.id", equalTo(hearingId.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(2).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalCourtCentre() {
        //given
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED_STR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                ALL_AUTHORITY_CODES_SEARCH,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED_STR,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(UNALLOCATED_STR,
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
        givenHearingsExist();

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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
        givenHearingsExist();

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
        givenHearingsExist();

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
        givenHearingsExist();

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
        givenHearingsExist();

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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
    public void shouldSaveAndFindHearingJsonWithHearingStartDateBetweenSearchDates() {
        //given
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
    public void shouldSaveAndFindHearingJsonWithHearingEndDateBetweenSearchDates() {
        //given
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
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
    public void shouldNotRetrieveVacatedHearingWhenVacatedIsTrueAndFindHearingsInvoked() {
        givenHearingsWithVacated(TRUE);

        final List<Hearing> actualHearings = hearingRepository.findHearings(RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                ALL_AUTHORITY_CODES_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveHearingWhenVacatedIsNullAndFindHearingsInvoked() {
        givenHearingsWithVacated(null);

        final List<Hearing> actualHearings = hearingRepository.findHearings(RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                ALL_AUTHORITY_CODES_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());


        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(OTHER_COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(OTHER_JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldRetrieveHearingWhenVacatedIsFalseAndFindHearingsInvoked() {
        givenHearingsWithVacated(FALSE);

        final List<Hearing> actualHearings = hearingRepository.findHearings(RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                ALL_AUTHORITY_CODES_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                to(START_SEARCH_DATE),
                EARLIEST_SEARCH_DATE_TIME.toString(),
                LATEST_SEARCH_DATE_TIME.toString());

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(OTHER_COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(OTHER_JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldRetrieveVacatedHearingWhenVacatedIsNullOrFalseAndFindHearingsByWeekCommencingRangeInvoked() {
        givenHearingsWithWeekCommencing(HEARING_ID, COURT_CENTRE_ID, AUTHORITY_ID, HEARING_TYPE, CROWN, JUDICIAL_ID, null);
        givenHearingsWithWeekCommencing(OTHER_HEARING_ID, OTHER_COURT_CENTRE_ID, AUTHORITY_ID, OTHER_HEARING_TYPE, CROWN, JUDICIAL_ID, FALSE);

        //when
        List<Hearing> actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                OTHER_COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                OTHER_HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
    }

    @Test
    public void shouldNotRetrieveVacatedHearingWhenVacatedIsTrueAndFindHearingsByWeekCommencingRangeInvoked() {
        givenHearingsWithWeekCommencing(HEARING_ID, COURT_CENTRE_ID, AUTHORITY_ID, HEARING_TYPE, CROWN, JUDICIAL_ID, TRUE);

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_CODE_SEARCH,
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                to(WEEK_COMMENCING_START_DATE),
                to(WEEK_COMMENCING_END_DATE));

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveHearingsWhenFindHearingsForPublicStandardListInvoked() {
        //given
        givenHearingsExist();

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
    public void shouldNotRetrieveVacatedHearingsWhenFindHearingsForPublicStandardListInvoked() {
        //given
        givenHearingsWithVacated(TRUE);

        //when
        final Hearing actualHearings = hearingRepository.findHearingsForPublicStandardList(
                RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                to(START_SEARCH_DATE),
                to(END_SEARCH_DATE));

        //then
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary", isEmptyOrNullString()));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings.size()", equalTo(1)));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId", isEmptyOrNullString()));
    }

    @Test
    public void shouldRetrieveHearingsWhenFindHearingsForAlphabeticalListInvoked() {
        givenHearingsExist();

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
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingsByHearingDate[0].hearing.hearingDays[0].startTime", equalTo(DAY_1_START_TIME.toString())));
    }

    @Test
    public void shouldNotRetrieveVacatedHearingsWhenFindHearingsForAlphabeticalListInvoked() {
        //given
        givenHearingsWithVacated(TRUE);

        //when
        final List<Hearing> hearingsForAlphabeticalList = hearingRepository.findHearingsForAlphabeticalList(
                RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                to(HEARING_DATE));

        //then
        assertThat(hearingsForAlphabeticalList.size(), is(1));
        final Hearing hearing = hearingsForAlphabeticalList.get(0);
        assertThat(hearing.getProperties(), nullValue());
    }

    @Test
    public void shouldSaveButNotFindAlphabeticalCourtList() {
        givenHearingsExist();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(RANDOM_ALLOCATED, COURT_CENTRE_ID.toString(), "1900-01-01");

        assertThat(foundHearings.get(0).getProperties(), nullValue());
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JurisdictionType.CROWN.name())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.caseReference", equalTo(CASE_REFERENCE)));

        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID2.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.endDate", equalTo(to(END_DATE))));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
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
                RANDOM_ALLOCATED,
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
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(RANDOM_ALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].linkedCases[0].caseUrn", equalTo(LINKED_CASE_URN)));
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
    public void shouldFindHearingsByCaseUrnAndAnyAllocationState() {

        //given
        givenVariousHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("45DI277164");

        //then
        assertThat(actualHearings.size(), is(3));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID.toString(), OTHER_HEARING_ID2.toString()));
        assertThat(extractFields(actualHearings, "$.allocated"), containsInAnyOrder(Boolean.TRUE.toString(), Boolean.FALSE.toString(), Boolean.FALSE.toString()));
        assertThat(actualHearings.get(0).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
        assertThat(actualHearings.get(1).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
        assertThat(actualHearings.get(1).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
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
        assertThat(extractFields(actualHearings, "$.id"), not(contains(VACATED_HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));

        // unset vacated status for hearing
        JsonEntityFinder.using(hearingRepository).find(VACATED_HEARING_ID)
                .put("isVacatedTrial", false)
                .save();

        final List<Hearing> actualHearingsPostUnsetStatus = hearingRepository.findHearings(null, null, Collections.singleton(COURT_CENTRE_ID.toString()));
        assertThat(actualHearingsPostUnsetStatus.size(), is(3));
        assertThat(extractFields(actualHearingsPostUnsetStatus, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID2.toString(), VACATED_HEARING_ID.toString()));
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

    @Test
    public void shouldFindAllocatedAndUnallocatedHearingsByCaseId() {

        //given
        givenAllocatedAndUnallocatedHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId("e2b13dc0-de95-11e8-9df5-e56feb0784f6",null);
        //then
        System.out.println("-->"+HEARING_ID.toString());
        System.out.println("-->"+OTHER_HEARING_ID.toString());

        System.out.println("==>"+extractFields(actualHearings, "$.id"));
        System.out.println("==>"+extractFields(actualHearings, "$.id"));

        assertThat(actualHearings.size(), is(2));
//        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.id", equalTo(OTHER_HEARING_ID.toString())));
//        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));


        assertTrue(extractFields(actualHearings, "$.id").contains(HEARING_ID.toString()));
         assertTrue(extractFields(actualHearings, "$.id").contains(OTHER_HEARING_ID.toString()));

    }

    @Test
    public void shouldFindAllocatedAndUnallocatedHearingsByApplicationId() {

        //given
        givenAllocatedAndUnallocatedHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null,"e02b8109-9b61-42b9-8741-2283d832ec19");
        //then
        assertThat(actualHearings.size(), is(2));
        assertTrue(extractFields(actualHearings, "$.id").contains(HEARING_ID.toString()));
        assertTrue(extractFields(actualHearings, "$.id").contains(OTHER_HEARING_ID.toString()));

    }
    private List<Hearing> givenAllocatedAndUnallocatedHearings() {

        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(ALLOCATED)
                .withVacated(true)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenAvailableHearings() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE)
                .withStartTimeDay3(DAY_3_START_TIME)
                .withEndTimeDay3(DAY_3_END_TIME)
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE)
                .withStartTimeDay3(DAY_3_START_TIME)
                .withEndTimeDay3(DAY_3_END_TIME)
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));
        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenAvailableHearingsForCrownAndMags() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID2)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenHearingsWithVacated(final Boolean vacated) {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(vacated)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(nonNull(vacated) ? TEST_DATA_SAMPLE_HEARING_JSON : TEST_DATA_SAMPLE_HEARING_NULL_VACATED_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenHearingsExist() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE)
                .withStartTimeDay3(DAY_3_START_TIME)
                .withEndTimeDay3(DAY_3_END_TIME)
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE)
                .withStartTimeDay3(DAY_3_START_TIME)
                .withEndTimeDay3(DAY_3_END_TIME)
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private void givenVariousHearings() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(true)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID2)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(randomUUID())
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(false)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON).build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(randomUUID())
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(false)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON).build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));

    }

    private List<Hearing> givenHearingsExistWithDifferentStartAndEndDates() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE)
                .withStartTimeDay3(DAY_3_START_TIME)
                .withEndTimeDay3(DAY_3_END_TIME)
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(RANDOM_ALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE.plusDays(1))
                .withHearingDateDay1(DAY_1_HEARING_DATE)
                .withStartTimeDay1(DAY_1_START_TIME)
                .withEndTimeDay1(DAY_1_END_TIME)
                .withCancelledDay1(false)
                .withHearingDateDay2(DAY_2_HEARING_DATE)
                .withStartTimeDay2(DAY_2_START_TIME)
                .withEndTimeDay2(DAY_2_END_TIME)
                .withCancelledDay2(false)
                .withHearingDateDay3(DAY_3_HEARING_DATE.plusDays(1))
                .withStartTimeDay3(DAY_3_START_TIME.plusDays(1))
                .withEndTimeDay3(DAY_3_END_TIME.plusDays(1))
                .withCancelledDay3(false)
                .withFileLocation(TEST_DATA_SAMPLE_MULTIDAY_HEARING_JSON)
                .withMultidayHearing(true)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenHearingsWithWeekCommencing(final UUID hearingId,
                                                          final UUID courtCentreId,
                                                          final UUID authorityId,
                                                          final Type hearingType,
                                                          final JurisdictionType jurisdictionType,
                                                          final String judicialId,
                                                          final Boolean vacated) {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(hearingId)
                .withCourtCentreId(courtCentreId)
                .withAllocated(UNALLOCATED)
                .withVacated(vacated)
                .withAuthorityId(authorityId)
                .withHearingType(hearingType)
                .withJurisdictionType(jurisdictionType)
                .withJudicialId(judicialId)
                .withWeekCommencingStartDate(WEEK_COMMENCING_START_DATE)
                .withWeekCommencingEndDate(WEEK_COMMENCING_END_DATE)
                .withVacated(vacated)
                .withFileLocation(SAMPLE_UNALLOCATED_HEARING_FOR_WEEK_COMMENCING)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenUnscheduledHearings() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(true)
                .withVacated(NOT_VACATED)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID2)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(randomUUID())
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(randomUUID())
                .withCourtCentreId(OTHER_COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(NOT_VACATED)
                .withAuthorityId(OTHER_AUTHORITY_ID)
                .withHearingType(OTHER_HEARING_TYPE)
                .withJurisdictionType(OTHER_JURISDICTION_TYPE)
                .withJudicialId(OTHER_JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(VACATED_HEARING_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withAllocated(UNALLOCATED)
                .withVacated(true)
                .withAuthorityId(AUTHORITY_ID)
                .withHearingType(HEARING_TYPE)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withJudicialId(JUDICIAL_ID)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .withHearingDate(HEARING_DATE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private Hearing getHearingJson(final HearingRepositoryContext context) {
        final String hearingString = createHearingString(context);
        return new HearingBuilder()
                .setId(context.getHearingId())
                .setProperties(toJsonNode(hearingString))
                .build();
    }

    private String createHearingString(final HearingRepositoryContext context) {
        String hearingString = getPayload(context.getFileLocation());
        String updatedHearingString = hearingString
                .replaceAll(HEARING_ID_FIELD, context.getHearingId().toString())
                .replaceAll(JURISDICTION_TYPE_FIELD, context.getJurisdictionType().toString())
                .replaceAll(JUDICIAL_ID_FIELD, context.getJudicialId())
                .replaceAll(ALLOCATED_FIELD, Boolean.toString(context.isAllocated()))
                .replaceAll(COURT_CENTRE_ID_FIELD, context.getCourtCentreId().toString())
                .replaceAll(AUTHORITY_ID_FIELD, context.getAuthorityId().toString())
                .replaceAll(HEARING_TYPE_ID_FIELD, context.getHearingType().getId().toString())
                .replaceAll(HEARING_TYPE_DESCRIPTION_FIELD, context.getHearingType().getDescription());

        updatedHearingString = updatedHearingString.replaceAll(VACATED_TRIAL_FIELD, nonNull(context.isVacated()) ? Boolean.toString(context.isVacated()) : "null");

        if (isNull(context.getWeekCommencingStartDate())) {
            updatedHearingString = updatedHearingString
                    .replaceAll(COURT_ROOM_ID_FIELD, context.getCourtRoomId().toString())
                    .replaceAll(START_DATE_FIELD, to(context.getStartDate()))
                    .replaceAll(END_DATE_FIELD, to(context.getEndDate()));
            if (context.isMultidayHearing()) {
                updatedHearingString = updatedHearingString
                        .replaceAll(DAY_1_HEARING_DATE_FIELD, to(context.getHearingDateDay1()))
                        .replaceAll(DAY_1_START_TIME_FIELD, context.getStartTimeDay1().toString())
                        .replaceAll(DAY_1_END_TIME_FIELD, context.getEndTimeDay1().toString())
                        .replaceAll(DAY_2_HEARING_DATE_FIELD, to(context.getHearingDateDay2()))
                        .replaceAll(DAY_2_START_TIME_FIELD, context.getStartTimeDay2().toString())
                        .replaceAll(DAY_2_END_TIME_FIELD, context.getEndTimeDay2().toString())
                        .replaceAll(DAY_3_HEARING_DATE_FIELD, to(context.getHearingDateDay3()))
                        .replaceAll(DAY_3_START_TIME_FIELD, context.getStartTimeDay3().toString())
                        .replaceAll(DAY_3_END_TIME_FIELD, context.getEndTimeDay3().toString());

                if (nonNull(context.isCancelledDay1())) {
                    updatedHearingString = updatedHearingString.replaceAll(DAY_1_IS_CANCELLED_FIELD, Boolean.toString(context.isCancelledDay1()));
                } else {
                    updatedHearingString = updatedHearingString.replaceAll(",\n\\s+\"isCancelled\": DAY_1_IS_CANCELLED_FIELD", "");
                }

                if (nonNull(context.isCancelledDay2())) {
                    updatedHearingString = updatedHearingString.replaceAll(DAY_2_IS_CANCELLED_FIELD, Boolean.toString(context.isCancelledDay2()));
                } else {
                    updatedHearingString = updatedHearingString.replaceAll(",\n\\s+\"isCancelled\": DAY_2_IS_CANCELLED_FIELD", "");
                }

                if (nonNull(context.isCancelledDay3())) {
                    updatedHearingString = updatedHearingString.replaceAll(DAY_3_IS_CANCELLED_FIELD, Boolean.toString(context.isCancelledDay3()));
                } else {
                    updatedHearingString = updatedHearingString.replaceAll(",\n\\s+\"isCancelled\": DAY_3_IS_CANCELLED_FIELD", "");
                }
            } else {
                updatedHearingString = updatedHearingString
                        .replaceAll(START_TIME_FIELD, context.getStartTime().toString())
                        .replaceAll(END_TIME_FIELD, context.getEndTime().toString())
                        .replaceAll(HEARING_DATE_FIELD, to(context.getHearingDate()));
            }
        } else {
            updatedHearingString = updatedHearingString
                    .replaceAll(WEEK_COMMENCING_START_FIELD, to(context.getWeekCommencingStartDate()))
                    .replaceAll(WEEK_COMMENCING_END_FIELD, to(context.getWeekCommencingEndDate()));
        }
        return updatedHearingString;
    }

}

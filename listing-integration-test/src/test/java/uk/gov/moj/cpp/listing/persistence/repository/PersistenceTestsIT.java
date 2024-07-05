package uk.gov.moj.cpp.listing.persistence.repository;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static com.vladmihalcea.hibernate.type.json.internal.JacksonUtil.toJsonNode;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static java.util.Collections.singleton;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.domain.Type.type;
import static uk.gov.moj.cpp.listing.persistence.repository.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.persistence.repository.utils.HearingRepositoryContext.hearingRepositoryContext;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.LinkedCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.persistence.repository.utils.FileUtil;
import uk.gov.moj.cpp.listing.persistence.repository.utils.HearingRepositoryContext;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import junit.framework.TestCase;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
* These repository tests needs a direct db connection and has been configured to use listingsystem .
* CdiTestRunner needs to update db according to entities in the repository and viewstore cannot be used  as IT test requires them for assertions
* */
@RunWith(CdiTestRunner.class)
public class PersistenceTestsIT extends BaseTransactionalTest implements PersistenceTestsInt {
    @Inject
    public HearingRepository hearingRepository;

    @Inject
    public ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    public ObjectMapper objectMapper;

    @Inject
    public PublishedCourtListRepository publishedCourtListRepository;

    @Inject
    NotesRepository notesRepository;

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
                randomUUID(),
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldSaveAndFindHearingWhereQueryFindsResultsJson() {
        //given
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                OTHER_HEARING_TYPE.getId(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveVacatedHearingWhenVacatedNullOrFalseAndFindHearingsInvoked() {
        //given
        givenHearingsWithVacated(null);

        //when
        List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findHearings(
                RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                OTHER_HEARING_TYPE.getId(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                AUTHORITY_ID.toString(),
                null,
                CROWN.toString(),
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE,
                0, 10);

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
                AUTHORITY_ID,
                null,
                CROWN.toString(),
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                false, 0, 10);

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
                COURT_CENTRE_ID,
                null,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                CROWN.toString(),
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                false, 0, 10);

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveHearingWhenVacatedIsNullOrFalseAndFindUnallocatedHearingsByWeekCommencingRangeInvoked() {
        givenHearingsWithWeekCommencing(HEARING_ID, COURT_CENTRE_ID, AUTHORITY_ID, HEARING_TYPE, CROWN, JUDICIAL_ID, FALSE);
        givenHearingsWithWeekCommencing(OTHER_HEARING_ID, OTHER_COURT_CENTRE_ID, AUTHORITY_ID, OTHER_HEARING_TYPE, CROWN, JUDICIAL_ID, null);

        List<Hearing> actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                COURT_CENTRE_ID,
                null,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                CROWN.toString(),
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                false, 0, 10);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                OTHER_COURT_CENTRE_ID,
                null,
                AUTHORITY_ID,
                OTHER_HEARING_TYPE.getId(),
                CROWN.toString(),
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                false, 0, 10);

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
                AUTHORITY_ID.toString(),
                null,
                null,
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE, 0, 10);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.id", equalTo(hearingId.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(1).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    @Test
    public void shouldSaveAndFindHearingJsonWithOptionalCourtCentre() {
        //given
        givenHearingsExist();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(
                UNALLOCATED_STR,
                null,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                COURT_CENTRE_ID,
                null,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE,0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                null,
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                COURT_CENTRE_ID,
                null,
                AUTHORITY_ID,
                null,
                null,
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE), 0, 10);

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
                null,
                null,
                null,
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                LATEST_SEARCH_DATE_TIME);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                null,
                START_SEARCH_DATE,
                END_SEARCH_DATE, 0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),0, 10);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                LATEST_SEARCH_DATE_TIME);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                END_TIME);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                START_TIME,
                LATEST_SEARCH_DATE_TIME);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                START_TIME.minusHours(5),
                END_TIME.minusHours(5));

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE.plusDays(10),
                END_SEARCH_DATE.plusDays(10), 0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE.minusDays(1),
                END_SEARCH_DATE, 0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE.plusDays(1),
                END_SEARCH_DATE.plusDays(2),0, 10);

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
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                AUTHORITY_ID,
                HEARING_TYPE.getId(),
                JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE.plusDays(1),
                END_SEARCH_DATE, 0, 10);

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
                null,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                LATEST_SEARCH_DATE_TIME);

        assertThat(actualHearings, empty());
    }

    @Test
    public void shouldRetrieveHearingWhenVacatedIsNullAndFindHearingsInvoked() {
        givenHearingsWithVacated(null);

        final List<Hearing> actualHearings = hearingRepository.findHearings(RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                null,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                LATEST_SEARCH_DATE_TIME);


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
                null,
                OTHER_HEARING_TYPE.getId().toString(),
                OTHER_JURISDICTION_TYPE.toString(),
                START_SEARCH_DATE,
                EARLIEST_SEARCH_DATE_TIME,
                LATEST_SEARCH_DATE_TIME);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE, 0, 10
        );

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));

        actualHearings = hearingRepository.findHearingsByWeekCommencingRange(
                OTHER_COURT_CENTRE_ID.toString(),
                null,
                AUTHORITY_ID.toString(),
                OTHER_HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE, 0, 10);

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
                AUTHORITY_ID.toString(),
                HEARING_TYPE.getId().toString(),
                CROWN.toString(),
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE, 0, 10);

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
                START_SEARCH_DATE,
                END_SEARCH_DATE);

        //then
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary.size()", equalTo(2)));
        //assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary[0].judicialId", equalTo(JUDICIAL_ID)));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings.size()", equalTo(1)));
        //assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId[0].hearingDate", equalTo(START_DATE.toString())));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId[0].hearingsByHearingDate[0].hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())));
    }

    public void givenHearingsWithMultipleCourtCentres(final UUID courtCentreId, final UUID courtRoomId, final UUID otherCourtCentreId, final LocalDate startDate, final LocalDate endDate) {
        final Hearing hearing1 = givenHearingWithCourtCentreDetails(courtCentreId, courtRoomId, MAGISTRATES, startDate, endDate, true);
        hearing1.setHearingDays(getHearingDays(courtCentreId, courtRoomId, otherCourtCentreId, startDate, endDate, hearing1));
        addHearingDays(courtCentreId, courtRoomId, otherCourtCentreId, startDate, endDate, hearing1);

        final Hearing hearing2 = givenHearingWithCourtCentreDetails(otherCourtCentreId, courtRoomId, MAGISTRATES, startDate, endDate, true);
        hearing2.setHearingDays(getHearingDays(otherCourtCentreId, courtRoomId, courtCentreId, startDate, endDate, hearing2));
        addHearingDays(otherCourtCentreId, courtRoomId, courtCentreId, startDate, endDate, hearing2);

        final Hearing hearing3 = givenHearingWithCourtCentreDetails(otherCourtCentreId, courtRoomId, MAGISTRATES, startDate, endDate, true);
        hearing3.setHearingDays(getHearingDays(otherCourtCentreId, courtRoomId, otherCourtCentreId, startDate, endDate, hearing3));
        addHearingDays(otherCourtCentreId, courtRoomId, otherCourtCentreId, startDate, endDate, hearing3);

        Stream.of(hearing1, hearing2, hearing3).forEach(hearing -> hearingRepository.save(hearing));
    }

    public void addHearingDays(final UUID courtCentreId, final UUID courtRoomId, final UUID otherCourtCentreId, final LocalDate startDate, final LocalDate endDate, final Hearing hearing1) {
        ((ObjectNode) hearing1.getProperties()).putArray("hearingDays").addAll(getHearingDays(Stream.of(
                HearingDay.hearingDay().withCourtCentreId(courtCentreId).withCourtRoomId(courtRoomId).withHearingDate(startDate).build(),
                HearingDay.hearingDay().withCourtCentreId(otherCourtCentreId).withCourtRoomId(courtRoomId).withHearingDate(endDate).build())
                .collect(Collectors.toList())));
    }

    public Set<HearingDays> getHearingDays(final UUID courtCentreId, final UUID courtRoomId, final UUID otherCourtCentreId, final LocalDate startDate, final LocalDate endDate, final Hearing hearing1) {
        return Stream.of(
                HearingDays.builder().withId(randomUUID()).withHearing(hearing1)
                        .withCourtCentreId(courtCentreId).withCourtRoomId(courtRoomId)
                        .withHearingDate(startDate).withDurationMinutes(30)
                        .withStartTime(ZonedDateTime.of(startDate, LocalTime.now(), UTC))
                        .withEndTime(ZonedDateTime.of(startDate, LocalTime.now().plusMinutes(30), UTC))
                        .withSequence(0).build(),
                HearingDays.builder().withId(randomUUID()).withHearing(hearing1)
                        .withCourtCentreId(otherCourtCentreId).withCourtRoomId(courtRoomId)
                        .withHearingDate(endDate).withDurationMinutes(30)
                        .withStartTime(ZonedDateTime.of(endDate, LocalTime.now(), UTC))
                        .withEndTime(ZonedDateTime.of(endDate, LocalTime.now().plusMinutes(30), UTC))
                        .withSequence(0).build())
                .collect(Collectors.toSet());
    }

    public Set<HearingDays> getHearingDays(final Hearing hearing) {
        final Set<HearingDays> hearingDays = new HashSet<>();
        final Iterator<JsonNode> hearingDaysIterator = hearing.getProperties().get("hearingDays").iterator();

        while (hearingDaysIterator.hasNext()) {
            final JsonNode hearingDay = hearingDaysIterator.next();
            final UUID courtCentreId = ofNullable(hearingDay.get("courtCentreId")).map(JsonNode::asText).map(UUID::fromString).orElse(null);
            final UUID courtRoomId = ofNullable(hearingDay.get("courtRoomId")).map(JsonNode::asText).map(UUID::fromString).orElse(null);
            final ZonedDateTime hearingDateTime = ZonedDateTime.parse(hearingDay.get("hearingDate").asText());
            hearingDays.add(HearingDays.builder().withId(randomUUID()).withHearing(hearing)
                        .withCourtCentreId(courtCentreId).withCourtRoomId(courtRoomId)
                        .withHearingDate(hearingDateTime.toLocalDate()).withDurationMinutes(30)
                        .withStartTime(hearingDateTime)
                        .withEndTime(hearingDateTime.plusMinutes(30))
                        .withSequence(0).build());
        }
        return hearingDays;
    }

    @Test
    public void shouldNotRetrieveVacatedHearingsWhenFindHearingsForPublicStandardListInvoked() {
        //given
        givenHearingsWithVacated(TRUE);

        //when
        final Hearing actualHearings = hearingRepository.findHearingsForPublicStandardList(
                RANDOM_ALLOCATED,
                OTHER_COURT_CENTRE_ID.toString(),
                START_SEARCH_DATE,
                END_SEARCH_DATE);

        //then
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.judiciary", isEmptyOrNullString()));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings.size()", equalTo(1)));
        assertThat(actualHearings.getProperties().toString(), hasJsonPath("$.hearings[0].hearingsByCourtCentreId", isEmptyOrNullString()));
    }

    @Test
    public void shouldRetrieveHearingsWhenFindHearingsForAlphabeticalListInvoked() {
        givenHearingsExist();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(UNALLOCATED, COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate());

        assertThat(foundHearings.size(), is(1));
        assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$.size()", equalTo(1)));
        //assertThat(foundHearings.get(0).getProperties().toString(), hasJsonPath("$[0].hearingDate", equalTo(to(HEARING_DATE))));
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
                HEARING_DATE.toLocalDate());

        //then
        assertThat(hearingsForAlphabeticalList.size(), is(1));
        final Hearing hearing = hearingsForAlphabeticalList.get(0);
        assertThat(hearing.getProperties(), nullValue());
    }

    @Test
    public void shouldSaveButNotFindAlphabeticalCourtList() {
        givenHearingsExist();

        final List<Hearing> foundHearings = hearingRepository.findHearingsForAlphabeticalList(RANDOM_ALLOCATED, COURT_CENTRE_ID.toString(), parse("1900-01-01"));

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
                null,
                now());

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
                null,
                now());

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
                caseUrnForLinkedCases,
                now());

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
                null,
                now());

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
                null,
                now());

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
                null,
                now());

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
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, null, 0, 10);

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
        givenUnscheduledHearings("46DI277164");

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("46DI277164", null, 0, 10);

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
        givenVariousHearings("46DI277164");

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("46DI277164");

        //then
        assertThat(actualHearings.size(), is(3));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID.toString(), OTHER_HEARING_ID2.toString()));
        assertThat(extractFields(actualHearings, "$.allocated"), containsInAnyOrder(TRUE.toString(), FALSE.toString(), FALSE.toString()));
        assertThat(actualHearings.get(0).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
        assertThat(actualHearings.get(1).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
        assertThat(actualHearings.get(1).getProperties(), isJson(withoutJsonPath("$.unscheduled")));
    }

    @Test
    public void shouldFindUnscheduledHearingsWithApplicationCaseReference() {

        //given
        givenUnscheduledHearings();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("TFL7328425-1", null, 0, 10);

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
        givenUnscheduledHearingsWithTypeListId();

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, TYPE_OF_LIST_ID.toString(), 0, 10);

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
        final List<Hearing> actualHearings = hearingRepository.findHearings(null, null, singleton(COURT_CENTRE_ID.toString()), 0, 10);

        //then
        assertThat(actualHearings.size(), is(2));
        assertThat(extractFields(actualHearings, "$.id"), containsInAnyOrder(HEARING_ID.toString(), OTHER_HEARING_ID2.toString()));
        assertThat(extractFields(actualHearings, "$.id"), not(contains(VACATED_HEARING_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.unscheduled", equalTo(true)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));

        final Hearing hearing = hearingRepository.findBy(VACATED_HEARING_ID);
        hearing.setIsVacatedTrial(false);
        hearingRepository.save(hearing);
        // unset vacated status for hearing
        JsonEntityFinder.using(hearingRepository).find(VACATED_HEARING_ID)
                .put("isVacatedTrial", false)
                .save();

        final List<Hearing> actualHearingsPostUnsetStatus = hearingRepository.findHearings(null, null, singleton(COURT_CENTRE_ID.toString()), 0, 10);
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
        givenUnscheduledHearings("45DI277164");

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearings("45DI277164", TYPE_OF_LIST_ID.toString(), singleton(COURT_CENTRE_ID.toString()), 0, 10);

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
        givenAllocatedAndUnallocatedHearings(LISTED_CASES_ID_2, COURT_APPLICATION_ID_2);

        //when
        final List<Hearing> actualHearings = hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(LISTED_CASES_ID_2.toString());
        //then
        System.out.println("-->" + HEARING_ID.toString());

        System.out.println("==>" + extractFields(actualHearings, "$.id"));

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.id", equalTo(HEARING_ID.toString())));
        assertTrue(extractFields(actualHearings, "$.id").contains(HEARING_ID.toString()));

    }

    public List<Hearing> givenHearingsWithVacated(final Boolean vacated) {
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
                .withIsPossibleDisqualification(IS_POSSIBLE_DISQUALIFICATION)
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
                .withIsPossibleDisqualification(IS_POSSIBLE_DISQUALIFICATION)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }


    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForPublicCourList_1() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID courtCentreId = multipleCourtCentre.getCourtCentreId();
        LocalDate startDate = multipleCourtCentre.getStartDate();

        //when
        final Hearing publicCourtListData = hearingRepository.findHearingsForPublicStandardList(
                true,
                courtCentreId.toString(),
                startDate,
                startDate);

        //then
        final ArrayNode publicHearings = ((ArrayNode) publicCourtListData.getProperties().get("hearings").get(0).get("hearingsByCourtCentreId"));
        assertThat(publicHearings.size(), is(1));
        assertThat(publicHearings.get(0).get("hearingsByHearingDate").size(), is(1));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForAlphabeticalCourList_1() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID courtCentreId = multipleCourtCentre.getCourtCentreId();
        LocalDate startDate = multipleCourtCentre.getStartDate();

        //when
        final List<Hearing> alphabeticalHearings = hearingRepository.findHearingsForAlphabeticalList(
                true,
                courtCentreId.toString(),
                startDate);

        //then
        assertThat(alphabeticalHearings.get(0).getProperties().get(0).get("hearingsByHearingDate").size(), is(1));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForPublicCourList_2() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID courtCentreId = multipleCourtCentre.getCourtCentreId();
        LocalDate endDate = multipleCourtCentre.getEndDate();

        //when
        final Hearing publicCourtListData = hearingRepository.findHearingsForPublicStandardList(
                true,
                courtCentreId.toString(),
                endDate,
                endDate);

        //then
        final ArrayNode publicHearings = ((ArrayNode) publicCourtListData.getProperties().get("hearings").get(0).get("hearingsByCourtCentreId"));
        assertThat(publicHearings.size(), is(1));
        assertThat(publicHearings.get(0).get("hearingsByHearingDate").size(), is(1));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForAlphabeticalCourList_2() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID courtCentreId = multipleCourtCentre.getCourtCentreId();
        LocalDate endDate = multipleCourtCentre.getEndDate();

        //when
        final List<Hearing> alphabeticalHearings = hearingRepository.findHearingsForAlphabeticalList(
                true,
                courtCentreId.toString(),
                endDate);

        //then
        assertThat(alphabeticalHearings.get(0).getProperties().get(0).get("hearingsByHearingDate").size(), is(1));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForPublic_3() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID courtCentreId = multipleCourtCentre.getCourtCentreId();
        LocalDate startDate = multipleCourtCentre.getStartDate();
        LocalDate endDate = multipleCourtCentre.getEndDate();

        //when
        final Hearing actualHearings = hearingRepository.findHearingsForPublicStandardList(
                true,
                courtCentreId.toString(),
                startDate,
                endDate);

        //then
        final ArrayNode hearings = ((ArrayNode) actualHearings.getProperties().get("hearings").get(0).get("hearingsByCourtCentreId"));
        assertThat(hearings.size(), is(2));
        assertThat(hearings.get(0).get("hearingsByHearingDate").size(), is(1));
        assertThat(hearings.get(1).get("hearingsByHearingDate").size(), is(1));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForPublic_4() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID otherCourtCentreId = multipleCourtCentre.getOtherCourtCentreId();
        LocalDate startDate = multipleCourtCentre.getStartDate();
        LocalDate endDate = multipleCourtCentre.getEndDate();

        //when
        final Hearing actualHearings = hearingRepository.findHearingsForPublicStandardList(
                true,
                otherCourtCentreId.toString(),
                startDate,
                endDate);

        //then
        final ArrayNode hearings = ((ArrayNode) actualHearings.getProperties().get("hearings").get(0).get("hearingsByCourtCentreId"));
        assertThat(hearings.size(), is(2));
        assertThat(hearings.get(0).get("hearingsByHearingDate").size(), is(2));
        assertThat(hearings.get(1).get("hearingsByHearingDate").size(), is(2));
    }

    @Test
    public void shouldSaveAndFindHearingsWithMultipleCourtCentresForAlphabetical_3() {

        MultipleCourtCentre multipleCourtCentre = new MultipleCourtCentre(this).invoke();

        UUID otherCourtCentreId = multipleCourtCentre.getOtherCourtCentreId();
        LocalDate startDate = multipleCourtCentre.getStartDate();

        //when
        final List<Hearing> alphabeticalHearings = hearingRepository.findHearingsForAlphabeticalList(
                true,
                otherCourtCentreId.toString(),
                startDate);

        //then
        assertThat(alphabeticalHearings.get(0).getProperties().get(0).get("hearingsByHearingDate").size(), is(2));
    }

    @Test
    public void shouldFindAllocatedAndUnallocatedHearingsByApplicationId() {

        //given
        givenAllocatedAndUnallocatedHearings(LISTED_CASES_ID_1, COURT_APPLICATION_ID_1);

        //when
        final List<Hearing> actualHearings = hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null, COURT_APPLICATION_ID_1.toString());
        //then
        assertThat(actualHearings.size(), is(1));
        assertTrue(extractFields(actualHearings, "$.id").contains(OTHER_HEARING_ID.toString()));

    }

    @Test
    public void shouldReturnEmptyHearingsForCotrWhereQueryDoesNotFindResults() {
        givenHearingsExist();

        final Set<String> hearingTypeIds = new HashSet<>();
        hearingTypeIds.add("bf8155e1-90b9-4080-b133-bfbad895d6e4");

        final List<Hearing> actualHearings = hearingRepository.findHearingsForCotr(hearingTypeIds, randomUUID().toString(), START_SEARCH_DATE, END_SEARCH_DATE);

        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldSaveAndFindHearingsForCotrWhereQueryFindsResultsJson() {
        //given
        givenHearingsExist();

        final Set<String> hearingTypeIds = new HashSet<>();
        hearingTypeIds.add("bf8155e1-90b9-4080-b133-bfbad895d6e4");

        //when
        final List<Hearing> actualHearings = hearingRepository.findHearingsForCotr(hearingTypeIds, COURT_CENTRE_ID.toString(), START_SEARCH_DATE, END_SEARCH_DATE);

        //then
        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.allocated", equalTo(UNALLOCATED)));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.listedCases[0].caseIdentifier.authorityId", equalTo(AUTHORITY_ID.toString())));
        assertThat(actualHearings.get(0).getProperties().toString(), hasJsonPath("$.jurisdictionType", equalTo(JURISDICTION_TYPE.toString())));
    }

    // Published court list  rep test


    @Test
    public void shouldSuccessfullySaveWhenNoRecordExistsForPrimaryKey() throws Exception {


        final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );
        final PublishedCourtList proposedPublishedCourtList
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentTwo(), LAST_UPDATED, LAST_EXPORTED, courtListId);


        assertNull(publishedCourtListRepository.findBy(publishedCourtListPrimaryKey));
        final PublishedCourtList savedPublishedCourtList = publishedCourtListRepository.save(proposedPublishedCourtList);
        assertEquals(proposedPublishedCourtList, savedPublishedCourtList);

        final PublishedCourtList returnedPublishedCourtListForFirstTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtList));
        assertEquals(proposedPublishedCourtList, returnedPublishedCourtListForFirstTime);

    }

    @Test
    public void shouldSuccessfullySaveEvenWhenARecordAlreadyExistsForPrimaryKey() throws Exception {

        final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );
        final PublishedCourtList proposedPublishedCourtListHavingContentOne
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentOne(), LAST_UPDATED, LAST_EXPORTED, courtListId);
        final PublishedCourtList proposedPublishedCourtListHavingContentTwo
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentTwo(), LAST_UPDATED, LAST_EXPORTED, courtListId);

        assertNull(publishedCourtListRepository.findBy(publishedCourtListPrimaryKey));

        publishedCourtListRepository.save(proposedPublishedCourtListHavingContentOne);

        final PublishedCourtList foundPublishedCourtListForFirstTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtListHavingContentOne));
        assertEquals(proposedPublishedCourtListHavingContentOne, foundPublishedCourtListForFirstTime);

        publishedCourtListRepository.save(proposedPublishedCourtListHavingContentTwo);

        final PublishedCourtList foundPublishedCourtListForSecondTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtListHavingContentTwo));

        assertEquals(proposedPublishedCourtListHavingContentTwo, foundPublishedCourtListForSecondTime);

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentCourtCentreIds() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_TWO,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), Matchers.is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), Matchers.is(2L));

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentTypes() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FIRM,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), Matchers.is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), Matchers.is(2L));

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentStartDates() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                BOXING_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), Matchers.is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), Matchers.is(2L));

    }

    private void assertFoundAsExpected(final PublishedCourtList expectedPublishedCourtList, final PublishedCourtListPrimaryKey primaryKey) {
        assertEquals(publishedCourtListRepository.findBy(primaryKey), publishedCourtListRepository.findBy(primaryKey));
    }

    private PublishedCourtList generateAndSave(final PublishedCourtListPrimaryKey primaryKey, final JsonNode content) {
        final PublishedCourtList proposedPublishedCourtList
                = generateProposedPublishedCourtList(primaryKey, content, LAST_UPDATED, LAST_EXPORTED, courtListId);
        final PublishedCourtList savedPublishedCourtList
                = publishedCourtListRepository.save(proposedPublishedCourtList);
        assertEquals(proposedPublishedCourtList, savedPublishedCourtList);
        return savedPublishedCourtList;
    }

    private JsonNode getContentOne() throws IOException {
        return new ObjectMapper().readTree(FileUtil.getPayload("test-data/courtListJson1.txt"));
    }

    private JsonNode getContentTwo() throws IOException {
        return new ObjectMapper().readTree(FileUtil.getPayload("test-data/courtListJson2.txt"));
    }

    private PublishedCourtListPrimaryKey toKey(final PublishedCourtList publishedCourtList) {
        return new PublishedCourtListPrimaryKey(
                publishedCourtList.getCourtCentreId(),
                publishedCourtList.getPublishCourtListType(),
                publishedCourtList.getStartDate());
    }

    private void assertEquals(final PublishedCourtList expectedPublishedCourtList, final PublishedCourtList savedPublishedCourtList) {

        assertThat(savedPublishedCourtList.getCourtCentreId(), Matchers.is(expectedPublishedCourtList.getCourtCentreId()));
        assertThat(savedPublishedCourtList.getPublishCourtListType(), is(expectedPublishedCourtList.getPublishCourtListType()));
        assertThat(savedPublishedCourtList.getStartDate(), Matchers.is(expectedPublishedCourtList.getStartDate()));
        assertThat(savedPublishedCourtList.getCourtListJson(), Matchers.is(expectedPublishedCourtList.getCourtListJson()));
    }

    private PublishedCourtList generateProposedPublishedCourtList(
            final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey,
            final JsonNode getCourtListJson,
            final ZonedDateTime lastUpdated,
            final ZonedDateTime lastExported,
            final UUID courtListId) {
        return new PublishedCourtList(
                publishedCourtListPrimaryKey.getCourtCentreId(),
                publishedCourtListPrimaryKey.getPublishCourtListType(),
                publishedCourtListPrimaryKey.getStartDate(),
                getCourtListJson,
                lastUpdated,
                lastExported,
                courtListId
        );
    }

    //  PublishedCourtListRepositoryTest end

    // Notes Rep test start
    @Test
    public void shouldFindNotesId() {

        List<Notes> expectedNotes = IntStream.range(0, 2).mapToObj(i -> new Notes(randomUUID(), randomUUID(), LocalDate.now(), STRING.next())).
                peek(note -> notesRepository.save(note)).
                collect(Collectors.toList());

        List<Notes> actualNotes = notesRepository.findNotes(expectedNotes.stream().map(note -> note.getId()).collect(Collectors.toList()));

        TestCase.assertTrue(EqualsBuilder.reflectionEquals(expectedNotes, actualNotes));

    }

    @Test
    public void shouldFindNoteByCourtRoomAndDate() {
        final Notes notes = new Notes();
        final LocalDate date = LocalDate.now();
        notes.setDate(date);
        notes.setNote("Note description");
        notes.setId(randomUUID());
        final UUID courtRoomId = randomUUID();
        notes.setCourtRoomId(courtRoomId);
        notesRepository.save(notes);

        final List<Notes> byCourtRoomCourtCentreAndDate = notesRepository.findByCourtRoomIdAndDate(courtRoomId, date);
        Assert.assertThat(byCourtRoomCourtCentreAndDate.size(), is(1));
    }

    @Test
    public void shouldNotFindNoteByCourtRoomCourtCentreAndDate() {
        final List<Notes> byCourtRoomCourtCentreAndDate = notesRepository.findByCourtRoomIdAndDate(randomUUID(), LocalDate.now());
        Assert.assertThat(byCourtRoomCourtCentreAndDate.size(), is(0));
    }


    @Test
    public void shouldFindNoteById() {

        //Given
        UUID noteId = randomUUID();
        String noteDescription = "random note description";
        Notes note = createNoteObject(noteId, LocalDate.now(), randomUUID(), noteDescription);
        notesRepository.save(note);

        //When
        Notes optionalById = notesRepository.findOptionalById(noteId);

        //Then
        Assert.assertThat(optionalById.getId(), is(noteId));
        Assert.assertThat(optionalById.getNote(), is("random note description"));
    }

    @Test
    public void shouldlUpdateNoteDescription() {

        //Given
        UUID noteId = randomUUID();
        String noteDescription = "random note description";
        Notes note = createNoteObject(noteId, LocalDate.now(), randomUUID(), noteDescription);
        notesRepository.save(note);

        //When
        Notes optionalById = notesRepository.findOptionalById(noteId);
        optionalById.setNote("edited note description");
        notesRepository.save(note);
        Notes noteAfterChangingDescription = notesRepository.findOptionalById(noteId);

        //Then
        Assert.assertThat(noteAfterChangingDescription.getNote(), is("edited note description"));
        List<Notes> allNotes = notesRepository.findAll();
        Assert.assertThat(allNotes.size(), is(1));

    }

    private Notes createNoteObject(UUID noteId, LocalDate now, UUID courtCentreId,
                                   String noteDescription) {
        return new Notes(noteId, courtCentreId, now, noteDescription);
    }

    private List<Hearing> givenAllocatedAndUnallocatedHearings(final UUID caseId, final UUID applicationId) {

        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJsonForUnAllocatedAllocated(hearingRepositoryContext()
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
                .withUnscheduled(UNSCHEDULED)
                .build(), caseId, randomUUID()));

        hearingsToBeCreated.add(getHearingJsonForUnAllocatedAllocated(hearingRepositoryContext()
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
                .withUnscheduled(UNSCHEDULED)
                .build(), randomUUID(), applicationId));

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

    private List<Hearing> givenHearingsExist() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        final Hearing hearing1 = getHearingJson(hearingRepositoryContext()
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
                .build());
        hearing1.setHearingDays(getHearingDays(hearing1));
        hearingsToBeCreated.add(hearing1);

        final Hearing hearing2 = getHearingJson(hearingRepositoryContext()
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
                .build());
        hearing2.setHearingDays(getHearingDays(hearing2));
        hearingsToBeCreated.add(hearing2);

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private Hearing givenHearingWithCourtCentreDetails(final UUID courtCentreId, final UUID courtRoomId, final JurisdictionType jurisdiction, final LocalDate startDate, final LocalDate endDate, final boolean allocated) {
        return createHearingJson(randomUUID(), courtCentreId, courtRoomId, allocated, null, null, jurisdiction, null, startDate, endDate, null, null, null, null, null, TEST_DATA_SAMPLE_HEARING_WITH_COURT_CENTRE_JSON);
    }

    private void givenVariousHearings(final String caseUrn) {
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
                .withUnscheduled(UNSCHEDULED)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build(), caseUrn));

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
                .withUnscheduled(UNSCHEDULED)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build(), caseUrn));

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
                .withUnscheduled(UNSCHEDULED)
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON).build(), caseUrn));

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
                .withUnscheduled(UNSCHEDULED)
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
                .withUnscheduled(UNSCHEDULED)
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

    private Hearing getHearingJsonForUnscheduledWIthTypeListId(final HearingRepositoryContext context) {
        final String hearingString = createHearingString(context);
        final Hearing hearing = Hearing.builder()
                .withId(context.getHearingId())
                .withProperties(toJsonNode(hearingString))
                .withTypeId(context.getHearingType().getId())
                .withCourtCentreId(context.getCourtCentreId())
                .withCourtRoomId(context.getCourtRoomId())
                .withStartDate(context.getStartDate())
                .withEndDate(context.getEndDate())
                .withIsVacatedTrial(context.isVacated())
                .withJurisdictionType(context.getJurisdictionType().toString())
                .withWeekCommencingStartDate(context.getWeekCommencingStartDate())
                .withWeekCommencingEndDate(context.getWeekCommencingEndDate())
                .withAllocated(context.isAllocated())
                .withTypeOfListId(context.getTypeOfListId())
                .withUnscheduled(context.isUnscheduled())
                .withIsPossibleDisqualification(context.isPossibleDisqualification())
                .build();
        hearing.setHearingDays(getHearingDays(hearing));
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setAuthorityId(context.getAuthorityId());
        caseIdentifier.setCaseReference(LINKED_CASE_URN);
        hearing.setListedCases(new HashSet<>(asList(getListedCases(hearing, caseIdentifier, randomUUID()))));
        hearing.setCourtApplications(new HashSet<>(asList(new CourtApplications(randomUUID(), randomUUID(), hearing, "appType", randomUUID(), "appRef", "appParticulars", false))));

        return hearing;
    }

    private Hearing getHearingJson(final HearingRepositoryContext context) {
        final String hearingString = createHearingString(context);
        final Hearing hearing = Hearing.builder()
                .withId(context.getHearingId())
                .withProperties(toJsonNode(hearingString))
                .withTypeId(context.getHearingType().getId())
                .withCourtCentreId(context.getCourtCentreId())
                .withCourtRoomId(context.getCourtRoomId())
                .withStartDate(context.getStartDate())
                .withEndDate(context.getEndDate())
                .withIsVacatedTrial(context.isVacated())
                .withAllocated(context.isAllocated())
                .withJurisdictionType(context.getJurisdictionType().toString())
                .withWeekCommencingStartDate(context.getWeekCommencingStartDate())
                .withWeekCommencingEndDate(context.getWeekCommencingEndDate())
                .withAllocated(context.isAllocated())
                .withTypeOfListId(context.getTypeOfListId())
                .withUnscheduled(context.isUnscheduled())
                .withIsPossibleDisqualification(context.isPossibleDisqualification())
                .build();

        hearing.setHearingDays(getHearingDays(hearing));
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setAuthorityId(context.getAuthorityId());
        caseIdentifier.setCaseReference(LINKED_CASE_URN);
        hearing.setListedCases(new HashSet<>(asList(getListedCases(hearing, caseIdentifier, randomUUID()))));
        hearing.setCourtApplications(new HashSet<>(asList(new CourtApplications(randomUUID(), randomUUID(), hearing, "appType", randomUUID(), "appRef", "appParticulars", false))));
        return hearing;
    }

    private ListedCases getListedCases(final Hearing hearing, final CaseIdentifier caseIdentifier, final UUID uuid) {
        ListedCases listedCase = new ListedCases(randomUUID(), uuid, caseIdentifier, null, hearing, null, null,null);

        final JsonNode properties = hearing.getProperties();
        if (properties.has("listedCases")) {
            final JsonNode listedCasesNode = properties.get("listedCases");

            if (listedCasesNode.has("caseIdentifier")) {
                final JsonNode caseIdentifierNode = listedCasesNode.get("caseIdentifier");
                final String caseReference = ofNullable(caseIdentifier.getCaseReference()).orElse(ofNullable(caseIdentifierNode.get("caseReference")).map(JsonNode::asText).orElse(null));
                final UUID authorityId = ofNullable(caseIdentifier.getAuthorityId()).orElse(ofNullable(caseIdentifierNode.get("authorityId")).map(JsonNode::asText).map(UUID::fromString).orElse(null));
                final String authorityCode = ofNullable(caseIdentifier.getAuthorityCode()).orElse(ofNullable(caseIdentifierNode.get("authorityCode")).map(JsonNode::asText).orElse(null));

                caseIdentifier.setCaseReference(caseReference);
                caseIdentifier.setAuthorityId(authorityId);
                caseIdentifier.setAuthorityCode(authorityCode);
                listedCase.setCaseIdentifier(caseIdentifier);
            }

            Iterator<JsonNode> linkedCases = listedCasesNode.get(0).has("linkedCases") ? listedCasesNode.get(0).get("linkedCases").iterator() : emptyIterator();
            Iterator<JsonNode> defendants = listedCasesNode.get(0).has("defendants") ? listedCasesNode.get(0).get("defendants").iterator() : emptyIterator();

            final Set<LinkedCase> allLinkedCases = new HashSet<>();
            final Set<Defendant> allDefendants = new HashSet<>();
            while (linkedCases.hasNext()) {
                final JsonNode linkedCase = linkedCases.next();
                LinkedCase linkCase = new LinkedCase();
                linkCase.setId(randomUUID());
                linkCase.setCaseId(randomUUID());
                linkCase.setCaseUrn(linkedCase.get("caseUrn").asText());
                linkCase.setListedCase(listedCase);

                allLinkedCases.add(linkCase);
            }

            while (defendants.hasNext()) {
                final JsonNode defendantNode = defendants.next();
                Defendant defendant = new Defendant();
                defendant.setId(randomUUID());
                defendant.setDefendantId(randomUUID());
                defendant.setMasterDefendantId(ofNullable(defendantNode.get("masterDefendantId")).map(JsonNode::asText).map(UUID::fromString).orElse(null));
                defendant.setListedCase(listedCase);

                allDefendants.add(defendant);
            }

            listedCase.setLinkedCases(allLinkedCases);
            listedCase.setDefendants(allDefendants);
        }

        return listedCase;
    }


    private Hearing getHearingJson(final HearingRepositoryContext context, final String caseUrn) {
        final String hearingString = createHearingString(context);
        final Hearing hearing = Hearing.builder()
                .withId(context.getHearingId())
                .withProperties(toJsonNode(hearingString))
                .withTypeId(context.getHearingType().getId())
                .withCourtCentreId(context.getCourtCentreId())
                .withCourtRoomId(context.getCourtRoomId())
                .withStartDate(context.getStartDate())
                .withEndDate(context.getEndDate())
                .withIsVacatedTrial(context.isVacated())
                .withJurisdictionType(context.getJurisdictionType().toString())
                .withWeekCommencingStartDate(context.getWeekCommencingStartDate())
                .withWeekCommencingEndDate(context.getWeekCommencingEndDate())
                .withAllocated(context.isAllocated())
                .withUnscheduled(context.isUnscheduled())
                .withTypeOfListId(context.getTypeOfListId())
                .withIsPossibleDisqualification(context.isPossibleDisqualification())
                .build();

        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setAuthorityId(context.getAuthorityId());
        caseIdentifier.setCaseReference(caseUrn);
        hearing.setListedCases(new HashSet<>(asList(getListedCases(hearing, caseIdentifier, randomUUID()))));
        hearing.setCourtApplications(new HashSet<>(asList(new CourtApplications(randomUUID(), randomUUID(), hearing, "appType", randomUUID(), "appRef", "appParticulars", false))));
        return hearing;
    }

    private Hearing getHearingJsonForUnAllocatedAllocated(final HearingRepositoryContext context, final UUID listedCaseId, final UUID applicationId) {
        final String hearingString = createHearingString(context);
        final Hearing hearing = Hearing.builder()
                .withId(context.getHearingId())
                .withProperties(toJsonNode(hearingString))
                .withTypeId(context.getHearingType().getId())
                .withCourtCentreId(context.getCourtCentreId())
                .withCourtRoomId(context.getCourtRoomId())
                .withStartDate(context.getStartDate())
                .withEndDate(context.getEndDate())
                .withIsVacatedTrial(context.isVacated())
                .withJurisdictionType(context.getJurisdictionType().toString())
                .withWeekCommencingStartDate(context.getWeekCommencingStartDate())
                .withWeekCommencingEndDate(context.getWeekCommencingEndDate())
                .withAllocated(context.isAllocated())
                .withUnscheduled(context.isUnscheduled())
                .withIsPossibleDisqualification(context.isPossibleDisqualification())
                .build();

        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setAuthorityId(context.getAuthorityId());
        hearing.setListedCases(new HashSet<>(asList(getListedCases(hearing, caseIdentifier, listedCaseId))));
        hearing.setCourtApplications(new HashSet<>(asList(new CourtApplications(randomUUID(), applicationId, hearing, "appType", randomUUID(), "appRef", "appParticulars", false))));
        return hearing;
    }

    private ArrayNode getHearingDays(List<HearingDay> hearingDays) {
        return objectMapper.valueToTree(hearingDays.stream().map(day -> HearingDay.hearingDay()
                .withDurationMinutes(30)
                .withEndTime(ZonedDateTime.of(day.getHearingDate(), LocalTime.MAX, UTC))
                .withCourtCentreId(day.getCourtCentreId())
                .withCourtRoomId(day.getCourtRoomId())
                .withHearingDate(day.getHearingDate())
                .withSequence(0)
                .withStartTime(ZonedDateTime.of(day.getHearingDate(), LocalTime.MIN, UTC))
                .build()).collect(Collectors.toList()));
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

    private List<Hearing> givenUnscheduledHearingsWithTypeListId() {
        final List<Hearing> hearingsToBeCreated = new ArrayList<>();
        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));


        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
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
                .withTypeOfListId(randomUUID())
                .build()));

        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
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
                .withTypeOfListId(randomUUID())
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
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
                .withTypeOfListId(randomUUID())
                .withFileLocation(TEST_DATA_SAMPLE_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJsonForUnscheduledWIthTypeListId(hearingRepositoryContext()
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
                .withTypeOfListId(randomUUID())
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
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
                .withUnscheduled(TRUE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_WITHOUT_CASE_HEARING_JSON)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .build(), "TFL7328425-1"));

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
                .withTypeOfListId(TYPE_OF_LIST_ID)
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
                .withTypeOfListId(TYPE_OF_LIST_ID)
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
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withUnscheduled(TRUE)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
    }

    private List<Hearing> givenUnscheduledHearings(final String caseUrn) {
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build(),caseUrn));

        hearingsToBeCreated.add(getHearingJson(hearingRepositoryContext()
                .withHearingId(OTHER_HEARING_ID)
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
                .withUnscheduled(TRUE)
                .withTypeOfListId(TYPE_OF_LIST_ID)
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build(),caseUrn));

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
                .withUnscheduled(TRUE)
                .withTypeOfListId(randomUUID())
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
                .withTypeOfListId(randomUUID())
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
                .withTypeOfListId(randomUUID())
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
                .withTypeOfListId(randomUUID())
                .withFileLocation(TEST_DATA_SAMPLE_UNSCHEDULED_HEARING_JSON)
                .build()));

        hearingsToBeCreated.forEach(hearingToBeCreated -> hearingRepository.save(hearingToBeCreated));
        return hearingsToBeCreated;
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
                        .replaceAll(DAY_1_HEARING_DATE_FIELD, context.getHearingDateDay1().toString())
                        .replaceAll(DAY_1_START_TIME_FIELD, context.getStartTimeDay1().toString())
                        .replaceAll(DAY_1_END_TIME_FIELD, context.getEndTimeDay1().toString())
                        .replaceAll(DAY_2_HEARING_DATE_FIELD, context.getHearingDateDay2().toString())
                        .replaceAll(DAY_2_START_TIME_FIELD, context.getStartTimeDay2().toString())
                        .replaceAll(DAY_2_END_TIME_FIELD, context.getEndTimeDay2().toString())
                        .replaceAll(DAY_3_HEARING_DATE_FIELD, context.getHearingDateDay3().toString())
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
                        .replaceAll(HEARING_DATE_FIELD, context.getHearingDate().toString())
                        .replaceAll(FIELD_2_HEARING_DATE, UTC_CLOCK.now().minusDays(10).toString());
            }
        } else {
            updatedHearingString = updatedHearingString
                    .replaceAll(WEEK_COMMENCING_START_FIELD, to(context.getWeekCommencingStartDate()))
                    .replaceAll(WEEK_COMMENCING_END_FIELD, to(context.getWeekCommencingEndDate()));
        }
        return updatedHearingString;
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
        return Hearing.builder()
                .withId(hearingId)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withJurisdictionType(jurisdictionType.toString())
                .withEndDate(endDate)
                .withStartDate(startDate)
                .withWeekCommencingEndDate(weekCommencingEndDate)
                .withWeekCommencingStartDate(weekCommencingStartDate)
                .withProperties(JacksonUtil.toJsonNode(hearingString))
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
                        .replaceAll(AUTHORITY_ID_FIELD, ofNullable(authorityId).map(UUID::toString).orElse(null))
                        .replaceAll(HEARING_TYPE_ID_FIELD, ofNullable(hearingType).map(type -> type.getId()).map(UUID::toString).orElse(null))
                        .replaceAll(HEARING_TYPE_DESCRIPTION_FIELD, ofNullable(hearingType).map(type -> type.getDescription()).orElse(null))
                        .replaceAll(START_DATE_FIELD, to(startDate))
                        .replaceAll(END_DATE_FIELD, to(endDate))
                        .replaceAll(START_TIME_FIELD, ofNullable(startTime).map(LocalDateTime::toString).orElse(null))
                        .replaceAll(END_TIME_FIELD, ofNullable(endTime).map(LocalDateTime::toString).orElse(null))
                        .replaceAll(HEARING_DATE_FIELD, ofNullable(hearingDate).map(LocalDate::toString).orElse(null))
                :
                updatedHearingString
                        .replaceAll(ALLOCATED_FIELD, Boolean.toString(allocated))
                        .replaceAll(COURT_CENTRE_ID_FIELD, courtCentreId.toString())
                        .replaceAll(AUTHORITY_ID_FIELD, ofNullable(authorityId).map(UUID::toString).orElse(null))
                        .replaceAll(HEARING_TYPE_ID_FIELD, ofNullable(hearingType).map(type -> type.getId()).map(UUID::toString).orElse(null))
                        .replaceAll(HEARING_TYPE_DESCRIPTION_FIELD, ofNullable(hearingType).map(type -> type.getDescription()).orElse(null))
                        .replaceAll(WEEK_COMMENCING_START_FIELD, ofNullable(weekCommencingStartDate).map(LocalDate::toString).orElse(null))
                        .replaceAll(WEEK_COMMENCING_END_FIELD, ofNullable(weekCommencingEndDate).map(LocalDate::toString).orElse(null));

    }
}

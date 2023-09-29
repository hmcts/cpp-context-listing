package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.it.util.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingStepsWithWeekCommencing.loadFixedHearingData;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingStepsWithWeekCommencing.updateLoadedFixedHearingToWeekCommencingHearing;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingStepsWithWeekCommencing.updatedHearingListedData;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingStepsWithWeekCommencing.verifyHearingListedForWeekCommencing;

import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ListCourtWeekCommencingHearingIT extends AbstractIT {
    private final static String WEEK_COMMENCING_END_DATE_FOR_ONE_WEEK = LocalDate.now().plusDays(7L).toString();
    private final static String WEEK_COMMENCING_END_DATE_FOR_TWO_WEEKS = LocalDate.now().plusDays(14L).toString();

    private static final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private static final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private List<HearingsData> hearingsData;
    private List<UpdatedHearingData> updatedHearingDataList;

    @Before
    public void initialize() {
        cleanListingTables();
        hearingsData = loadFixedHearingData();

        //update start date for a hearing
        final UpdatedHearingData updatedHearingData1 = updatedHearingListedData(hearingsData.get(3));

        //update fixed hearing to week commencing hearings
        final UpdatedHearingData updatedHearingData2 = updateLoadedFixedHearingToWeekCommencingHearing(hearingsData.get(5), WEEK_COMMENCING_END_DATE_FOR_ONE_WEEK, 1);
        final UpdatedHearingData updatedHearingData3 = updateLoadedFixedHearingToWeekCommencingHearing(hearingsData.get(6), WEEK_COMMENCING_END_DATE_FOR_TWO_WEEKS, 2);

        updatedHearingDataList = asList(updatedHearingData1, updatedHearingData2, updatedHearingData3);
    }

    @After
    public void tearDown() {
        cleanListingTables();
    }

    @Test
    public void shouldListHearingsWithinWeekCommencingDateRangeByRelevance() {
        final String weekCommencingSearchStartDate = now().minusDays(7).toString();
        final String weekCommencingSearchEndDate = now().plusDays(22).toString();

        final HearingsData hearingsData1 = hearingsData.get(0);
        final HearingsData hearingsData2 = hearingsData.get(1);
        final HearingsData hearingsData3 = hearingsData.get(2);
        final HearingsData hearingsData5 = hearingsData.get(4);

        final UpdatedHearingData updatedHearingData = updatedHearingDataList.get(0);
        final UpdatedHearingData firstUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(1);
        final UpdatedHearingData secondUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(2);

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(7)),
                withJsonPath("$.hearings[0].id", is(hearingsData5.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType", is(hearingsData5.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId", is(hearingsData5.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].startDate", is(hearingsData5.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[0].endDate", is(hearingsData5.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[1].id", is(hearingsData2.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[1].jurisdictionType", is(hearingsData2.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[1].courtCentreId", is(hearingsData2.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[1].startDate", is(hearingsData2.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[1].endDate", is(hearingsData2.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[2].id", is(hearingsData1.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[2].jurisdictionType", is(hearingsData1.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[2].courtCentreId", is(hearingsData1.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[2].startDate", is(hearingsData1.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[2].endDate", is(hearingsData1.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[3].id", is(hearingsData3.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[3].jurisdictionType", is(hearingsData3.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[3].courtCentreId", is(hearingsData3.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[3].startDate", is(hearingsData3.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[3].endDate", is(hearingsData3.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[4].id", is(updatedHearingData.getHearingId().toString())),
                withJsonPath("$.hearings[4].jurisdictionType", is(updatedHearingData.getJurisdictionType())),
                withJsonPath("$.hearings[4].courtCentreId", is(updatedHearingData.getCourtCentreId().toString())),
                withJsonPath("$.hearings[4].startDate", is(updatedHearingData.getStartDate())),
                withJsonPath("$.hearings[4].endDate", is(updatedHearingData.getEndDate())),
                withJsonPath("$.hearings[5].id", is(firstUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[5].jurisdictionType", is(firstUpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[5].courtCentreId", is(firstUpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[5].weekCommencingStartDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[5].weekCommencingEndDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
                withJsonPath("$.hearings[6].id", is(secondUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[6].jurisdictionType", is(secondUpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[6].courtCentreId", is(secondUpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[6].weekCommencingStartDate", is(secondUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[6].weekCommencingEndDate", is(secondUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
        };

        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, weekCommencingSearchEndDate, true, matchers);
    }

    @Test
    public void shouldListHearingsWithEndDateOrWeekCommencingDatesWithinWeekCommencingDateRangeByRelevance() {
        final String weekCommencingSearchStartDate = now().plusDays(4).toString();
        final String weekCommencingSearchEndDate = now().plusDays(11).toString();

        final HearingsData hearingsData1 = hearingsData.get(2);

        final UpdatedHearingData firstUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(1);

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(2)),
                withJsonPath("$.hearings[0].id", is(hearingsData1.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType", is(hearingsData1.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId", is(hearingsData1.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].startDate", is(hearingsData1.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[0].endDate", is(hearingsData1.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[1].id", is(firstUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[1].jurisdictionType", is(firstUpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[1].courtCentreId", is(firstUpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[1].weekCommencingStartDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[1].weekCommencingEndDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
        };

        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, weekCommencingSearchEndDate, true, matchers);
    }

    @Test
    public void shouldListHearingsWithStartDateOrWeekCommencingDatesWithinWeekCommencingDateRangeByRelevance() {
        final String weekCommencingSearchStartDate = now().plusDays(14).toString();
        final String weekCommencingSearchEndDate = now().plusDays(22).toString();

        final UpdatedHearingData updatedHearingData = updatedHearingDataList.get(0);

        final UpdatedHearingData UpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(2);

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(2)),
                withJsonPath("$.hearings[0].id", is(updatedHearingData.getHearingId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType", is(updatedHearingData.getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId", is(updatedHearingData.getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].startDate", is(updatedHearingData.getStartDate())),
                withJsonPath("$.hearings[0].endDate", is(updatedHearingData.getEndDate())),
                withJsonPath("$.hearings[1].id", is(UpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[1].jurisdictionType", is(UpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[1].courtCentreId", is(UpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[1].weekCommencingStartDate", is(UpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[1].weekCommencingEndDate", is(UpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
        };

        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, weekCommencingSearchEndDate, true, matchers);
    }

    @Test
    public void shouldListUnallocatedHearingsWithinWeekCommencingDateRange() {
        final String weekCommencingSearchStartDate = now().toString();
        final HearingsData hearingsData5 = hearingsData.get(4);

        final UpdatedHearingData firstUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(1);
        final UpdatedHearingData secondUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(2);

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(3)),
                withJsonPath("$.hearings[0].id", is(hearingsData5.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType", is(hearingsData5.getHearingData().get(0).getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId", is(hearingsData5.getHearingData().get(0).getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].startDate", is(hearingsData5.getHearingData().get(0).getHearingStartDate().toString())),
                withJsonPath("$.hearings[0].endDate", is(hearingsData5.getHearingData().get(0).getHearingEndDate().toString())),
                withJsonPath("$.hearings[1].id", is(firstUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[1].jurisdictionType", is(firstUpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[1].courtCentreId", is(firstUpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[1].weekCommencingStartDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[1].weekCommencingEndDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
                withJsonPath("$.hearings[2].id", is(secondUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[2].jurisdictionType", is(secondUpdatedHearingDataWithWeekCommencingDate.getJurisdictionType())),
                withJsonPath("$.hearings[2].courtCentreId", is(secondUpdatedHearingDataWithWeekCommencingDate.getCourtCentreId().toString())),
                withJsonPath("$.hearings[2].weekCommencingStartDate", is(secondUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[2].weekCommencingEndDate", is(secondUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
        };
        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, "", false, matchers);
    }

    @Test
    public void shouldReturnEmptyListWhenNoHearingWithingWeekCommencingDateRangeByRelevance() {
        final String weekCommencingSearchStartDate = now().minusDays(14).toString();
        final String weekCommencingSearchEndDate = now().minusDays(7).toString();

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(0)),
                withJsonPath("$.hearings", empty()),
        };

        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, weekCommencingSearchEndDate, true, matchers);
    }

    @Test
    public void shouldRetrieveCasesByDefendantAndHearingDateForUnallocatedHearing() {
        final String weekCommencingSearchStartDate = now().toString();

        final UpdatedHearingData firstUpdatedHearingDataWithWeekCommencingDate = updatedHearingDataList.get(1);

        final Matcher[] matchers = {withJsonPath("$.hearings", hasSize(3)),
                withJsonPath("$.hearings[1].id", is(firstUpdatedHearingDataWithWeekCommencingDate.getHearingId().toString())),
                withJsonPath("$.hearings[1].weekCommencingStartDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingStartDate())),
                withJsonPath("$.hearings[1].weekCommencingEndDate", is(firstUpdatedHearingDataWithWeekCommencingDate.getWeekCommencingEndDate())),
        };
        verifyHearingListedForWeekCommencing(Jurisdiction.CROWN.name(), weekCommencingSearchStartDate, "", false, matchers);

        final String caseId = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getCaseId().toString();
        final String urn = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getCaseReference();
        final String defendantId = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getDefendants().get(1).getDefendantId().toString();
        final String firstName = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getDefendants().get(1).getFirstName();
        final String lastName = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getDefendants().get(1).getLastName();
        final LocalDate dateOfBirth = hearingsData.get(5).getHearingData().get(0).getListedCases().get(1).getDefendants().get(1).getDateOfBirth();

        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();
        listCourtHearingSteps.verifyQueryAPIFindCaseByPersonDefendantAndHearingDateForUnallocatedHearing(caseId, urn, defendantId, firstName, lastName, dateOfBirth.toString());
    }

    private static void cleanListingTables() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        viewStoreCleaner.cleanViewStoreTables();
    }
}

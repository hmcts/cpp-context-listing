package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.SearchAvailableHearingIT.CASE_IN_HEARING;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class RangeSearchQueryForCourtCalendarIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
    }


    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParametersForMagsCourtcalendar() throws JsonProcessingException {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
        updateHearingSteps.whenJudiciaryIsChangedForHearings();
        updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
        final String payload = updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendar();

        final Map jObj = new ObjectMapper().readValue(payload, Map.class);
        assertThat(jObj, Matchers.is(notNullValue()));
        assertThat(jObj.get("pageCount"), Matchers.is(1));
        assertThat(jObj.get("results"), Matchers.is(1));
        List<Map> hearings = (List<Map>) jObj.get("hearings");
        assertThat(hearings, hasSize(1));

        assertThat(hearings.get(0).get("hearingDayCount"), Matchers.is(2));
        assertThat(hearings.get(0).get("hearingDayPosition"), Matchers.is(2));
        assertThat((List<Map>)hearings.get(0).get("hearingDays"), hasSize(1));
    }

    @Test
    public void shouldRangeSearchCourtCalendarForCrown() throws JsonProcessingException {

        final UUID hearingId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                null, null);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        ;
        final String payload = listCourtHearingSteps1.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendar();
        final Map jObj = new ObjectMapper().readValue(payload, Map.class);
        assertThat(jObj, Matchers.is(notNullValue()));
        assertThat(jObj.get("pageCount"), Matchers.is(1));
        assertThat(jObj.get("results"), Matchers.is(2));
        List<Map> hearings = (List<Map>) jObj.get("hearings");
        assertThat(hearings, hasSize(2));

        assertThat(hearings.get(0).get("hearingDayCount"), Matchers.is(2));
        assertThat(hearings.get(0).get("hearingDayPosition"), Matchers.is(1));
        assertThat((List<Map>)hearings.get(0).get("hearingDays"), hasSize(1));

        assertThat(hearings.get(1).get("hearingDayCount"), Matchers.is(2));
        assertThat(hearings.get(1).get("hearingDayPosition"), Matchers.is(2));
        assertThat((List<Map>)hearings.get(1).get("hearingDays"), hasSize(1));

    }

}

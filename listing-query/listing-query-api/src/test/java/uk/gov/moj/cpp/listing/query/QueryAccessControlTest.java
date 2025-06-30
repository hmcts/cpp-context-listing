package uk.gov.moj.cpp.listing.query;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.*;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;


public class QueryAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_QUERY_RANGE_SEARCH = "listing.range.search.hearings";
    private static final String ACTION_QUERY_SEARCH = "listing.search.hearings";
    private static final String ACTION_QUERY_UNSCHEDULED_SEARCH = "listing.unscheduled.search.hearings";
    private static final String RANDOM_GROUP = "Random group";
    private static final String ACTION_ALLOCATED_AND_UNALLOCATED_HEARINGS = "listing.allocated.and.unallocated.hearings";
    private static final String ACTION_SEARCH_CASE_BY_PERSON_DEFENDANT_AND_HEARING_DATE = "listing.get.cases-by-person-defendant";
    private static final String ACTION_SEARCH_CASE_BY_ORGANISATION_DEFENDANT_AND_HEARING_DATE = "listing.get.cases-by-organisation-defendant";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QueryAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToListHearing() {
        final Action action = createActionFor(ACTION_QUERY_RANGE_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, PRISON_ADMIN, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE, GROUP_POLICE_ADMIN, GROUP_VICTIMS_WITNESS_CARE_ADMIN, NON_CPS_PROSECUTOR_GROUP)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToListHearing() {
        final Action action = createActionFor(ACTION_QUERY_RANGE_SEARCH);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSearchHearing() {
        final Action action = createActionFor(ACTION_QUERY_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE, NON_CPS_PROSECUTOR_GROUP,OPERATIONAL_DELIVERY_ADMIN)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }


    @Test
    public void shouldNotAllowAuthorisedUserToSearchHearing() {
        final Action action = createActionFor(ACTION_QUERY_SEARCH);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSearchUnscheduledHearing() {
        final Action action = createActionFor(ACTION_QUERY_UNSCHEDULED_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSearchUnscheduledHearing() {
        final Action action = createActionFor(ACTION_QUERY_UNSCHEDULED_SEARCH);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldBeAsExpectedForListingSearchHearing() {
        assertAccessAsExpected("listing.search.hearing",
                COURT_CLERKS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS, SYSTEM_USERS, COURT_ASSOCIATE,OPERATIONAL_DELIVERY_ADMIN);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetAllocatedAndUnallocatedHearings() {
        final Action action = createActionFor(ACTION_ALLOCATED_AND_UNALLOCATED_HEARINGS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE, GROUP_POLICE_ADMIN, GROUP_VICTIMS_WITNESS_CARE_ADMIN, JUDGE, DJMC, DEPUTIES, RECORDERS, SYSTEM_USERS)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetAllocatedAndUnallocatedHearings() {
        final Action action = createActionFor(ACTION_ALLOCATED_AND_UNALLOCATED_HEARINGS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSearchCasesByPersonDefendantAndHearingDate() {
        final Action action = createActionFor(ACTION_SEARCH_CASE_BY_PERSON_DEFENDANT_AND_HEARING_DATE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, DEFENCE_LAWYER, CHAMBERS_CLERK, CHAMBERS_ADMIN, ADVOCATES)).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSearchCasesByPersonDefendantAndHearingDate() {
        final Action action = createActionFor(ACTION_SEARCH_CASE_BY_PERSON_DEFENDANT_AND_HEARING_DATE);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSearchCasesByOrganisationDefendantAndHearingDate() {
        final Action action = createActionFor(ACTION_SEARCH_CASE_BY_ORGANISATION_DEFENDANT_AND_HEARING_DATE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, DEFENCE_LAWYER, CHAMBERS_CLERK, CHAMBERS_ADMIN, ADVOCATES)).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSearchCasesByOrganisationDefendantAndHearingDate() {
        final Action action = createActionFor(ACTION_SEARCH_CASE_BY_ORGANISATION_DEFENDANT_AND_HEARING_DATE);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    private void assertAccessAsExpected(String actionName, String... expectedGroups) {
        final Action action = createActionFor(actionName);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, expectedGroups)).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }
}
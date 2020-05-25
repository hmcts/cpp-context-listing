package uk.gov.moj.cpp.listing.query;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_ADMINISTRATORS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_ASSOCIATE;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_CLERKS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.CPS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.CROWN_COURT_ADMIN;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.GROUP_POLICE_ADMIN;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.GROUP_VICTIMS_WITNESS_CARE_ADMIN;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LEGAL_ADVISERS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LISTING_OFFICERS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.NPS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.YOTS;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;


public class QueryAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_QUERY_RANGE_SEARCH = "listing.range.search.hearings";
    private static final String ACTION_QUERY_SEARCH = "listing.search.hearings";
    private static final String RANDOM_GROUP = "Random group";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void shouldAllowAuthorisedUserToListHearing() {
        final Action action = createActionFor(ACTION_QUERY_RANGE_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE, GROUP_POLICE_ADMIN, GROUP_VICTIMS_WITNESS_CARE_ADMIN)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToListHearing() {
        final Action action = createActionFor(ACTION_QUERY_RANGE_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, COURT_ADMINISTRATORS, COURT_CLERKS, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSearchHearing() {
        final Action action = createActionFor(ACTION_QUERY_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, YOTS, CPS, NPS, COURT_ASSOCIATE)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSearchHearing() {
        final Action action = createActionFor(ACTION_QUERY_SEARCH);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, COURT_ADMINISTRATORS, COURT_CLERKS, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldBeAsExpectedForListingSearchHearing() {
        assertAccessAsExpected("listing.search.hearing",
                COURT_CLERKS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS);
    }

    private void assertAccessAsExpected(String actionName, String... expectedGroups) {
        final Action action = createActionFor(actionName);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, expectedGroups)).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }
}
package uk.gov.moj.cpp.listing.query.api;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_ADMINISTRATORS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_CLERKS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.CROWN_COURT_ADMIN;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LEGAL_ADVISERS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LISTING_OFFICERS;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldBeAsExpectedForListingSearchHearings() {
        assertAccessAsExpected("listing.search.hearings",
                COURT_CLERKS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS);

    }

    @Test
    public void shouldBeAsExpectedForListingRangeSearchHearings() {
        assertAccessAsExpected("listing.range.search.hearings",
                COURT_CLERKS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS);

    }

    @Test
    public void shouldBeAsExpectedForListingPublicList() {
        assertAccessAsExpected("listing.public.list",
                COURT_CLERKS, COURT_ADMINISTRATORS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS);
    }

    @Test
    public void shouldBeAsExpectedForListingSearchCourtList() {
        // Note that this is the outlier, in that it doesn't support COURT_ADMINISTRATORS.
        assertAccessAsExpected("listing.search.court.list",
                COURT_CLERKS, CROWN_COURT_ADMIN, LISTING_OFFICERS, LEGAL_ADVISERS);

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


    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
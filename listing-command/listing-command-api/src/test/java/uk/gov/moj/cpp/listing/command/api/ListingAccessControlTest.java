package uk.gov.moj.cpp.listing.command.api;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.*;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class ListingAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_SEND_CASE_FOR_LISTING = "listing.command.send-case-for-listing";
    private static final String RANDOM_GROUP = "Random group";


    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToSendCaseForListing() {
        final Action action = createActionFor(ACTION_SEND_CASE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_CLERKS))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToSendCaseForListing() {
        final Action action = createActionFor(ACTION_SEND_CASE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
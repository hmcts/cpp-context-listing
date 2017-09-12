package uk.gov.moj.cpp.listing.command.api;

import static org.mockito.BDDMockito.given;

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

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToSendCaseForListing() {
        final Action action = createActionFor(ACTION_SEND_CASE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Listing Officers",
                "Crown Court Admin"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToSendCaseForListing() {
        final Action action = createActionFor(ACTION_SEND_CASE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group")).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
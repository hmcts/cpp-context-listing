package uk.gov.moj.cpp.listing.command.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import java.util.Map;
import static org.mockito.BDDMockito.given;

public class ReferenceDataAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_NAME_ADD_JUDGE = "listing.command.add-judge";
    private static final String ACTION_NAME_ADD_COURT_CENTRE = "listing.command.add-court-centre";
    private static final String ACTION_NAME_ADD_COURT_ROOM = "listing.command.add-court-room";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToAddJudge() {
        final Action action = createActionFor(ACTION_NAME_ADD_JUDGE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToAddJudge() {
        final Action action = createActionFor(ACTION_NAME_ADD_JUDGE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group")).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToAddCourtCentre() {
        final Action action = createActionFor(ACTION_NAME_ADD_COURT_CENTRE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCourtCentre() {
        final Action action = createActionFor(ACTION_NAME_ADD_COURT_CENTRE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group")).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToAddCourtRoom() {
        final Action action = createActionFor(ACTION_NAME_ADD_COURT_ROOM);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCourtRoom() {
        final Action action = createActionFor(ACTION_NAME_ADD_COURT_ROOM);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group")).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
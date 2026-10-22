package uk.gov.moj.cpp.listing.command.api;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_ADMINISTRATORS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_ASSOCIATE;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.COURT_CLERKS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.CROWN_COURT_ADMIN;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LEGAL_ADVISERS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.LISTING_OFFICERS;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class HearingOffencesAndDefendantsAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    private static final String ACTION_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    private static final String ACTION_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    private static final String ACTION_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public HearingOffencesAndDefendantsAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToAddOffencesForHearing() {
        assertAuthorisedUserIsAllowed(ACTION_ADD_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToAddOffencesForHearing() {
        assertUnauthorisedUserIsRefused(ACTION_ADD_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldAllowAuthorisedUserToDeleteOffencesForHearing() {
        assertAuthorisedUserIsAllowed(ACTION_DELETE_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToDeleteOffencesForHearing() {
        assertUnauthorisedUserIsRefused(ACTION_DELETE_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldAllowAuthorisedUserToUpdateOffencesForHearing() {
        assertAuthorisedUserIsAllowed(ACTION_UPDATE_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToUpdateOffencesForHearing() {
        assertUnauthorisedUserIsRefused(ACTION_UPDATE_OFFENCES_FOR_HEARING);
    }

    @Test
    public void shouldAllowAuthorisedUserToUpdateDefendantsForHearing() {
        assertAuthorisedUserIsAllowed(ACTION_UPDATE_DEFENDANTS_FOR_HEARING);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToUpdateDefendantsForHearing() {
        assertUnauthorisedUserIsRefused(ACTION_UPDATE_DEFENDANTS_FOR_HEARING);
    }

    private void assertAuthorisedUserIsAllowed(final String actionName) {
        final Action action = createActionFor(actionName);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    private void assertUnauthorisedUserIsRefused(final String actionName) {
        final Action action = createActionFor(actionName);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}

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

    private static final String ACTION_LIST_COURT_HEARING = "listing.command.list-court-hearing";
    private static final String ACTION_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    private static final String ACTION_CHANGE_JUDICIARY_FOR_HEARING = "listing.command.change-judiciary-for-hearings";
    private static final String ACTION_SEQUENCE_HEARINGS = "listing.command.sequence-hearings";
    private static final String ACTION_RESTRICT_COURT_LIST="listing.command.restrict-court-list" ;
    private static final String RANDOM_GROUP = "Random group";


    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToListCourtHearing() {
        final Action action = createActionFor(ACTION_LIST_COURT_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, LEGAL_ADVISERS, COURT_CLERKS ))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToListCourtHearing() {
        final Action action = createActionFor(ACTION_LIST_COURT_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, COURT_ADMINISTRATORS, COURT_CLERKS, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToUpdateHearingForListing() {
        final Action action = createActionFor(ACTION_UPDATE_HEARING_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToChangeJudiciaryForHearing() {
        final Action action = createActionFor(ACTION_CHANGE_JUDICIARY_FOR_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSequenceHearings() {
        final Action action = createActionFor(ACTION_SEQUENCE_HEARINGS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToRestrictCourtList() {
        final Action action = createActionFor(ACTION_RESTRICT_COURT_LIST);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
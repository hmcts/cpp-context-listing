package uk.gov.moj.cpp.listing.command.api;

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
import static uk.gov.moj.cpp.listing.domain.RuleConstants.MAGISTRATES;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.NON_CPS_PROSECUTOR_GROUP;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.NPS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.SYSTEM_USERS;
import static uk.gov.moj.cpp.listing.domain.RuleConstants.YOTS;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.listing.domain.RuleConstants;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class ListingAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_LIST_COURT_HEARING = "listing.command.list-court-hearing";
    private static final String ACTION_LIST_UNSCHEDULED_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    private static final String ACTION_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    private static final String ACTION_CHANGE_JUDICIARY_FOR_HEARING = "listing.command.change-judiciary-for-hearings";
    private static final String ACTION_SEQUENCE_HEARINGS = "listing.command.sequence-hearings";
    private static final String ACTION_RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    private static final String ACTION_PUBLISH_COURT_LIST = "listing.command.publish-court-list";
    private static final String ACTION_PUBLISH_COURT_LISTS_FOR_CROWN_COURTS = "listing.command.publish-court-lists-for-crown-courts";
    private static final String ACTION_COURT_LIST_REQUEST_EXPORT = "listing.command.court-list-request-export";
    private static final String RANDOM_GROUP = "Random group";
    private static final String ACTION_EDIT_NOTE_FOR_LISTING = "listing.command.edit-listing-note";
    private static final String ACTION_CREATE_LISTING_NOTE = "listing.command.create-listing-note";
    private static final String ACTION_DELETE_LISTING_NOTE = "listing.command.delete-listing-note";
    private static final String ACTION_MARK_UNALLOCATED_HEARING_AS_DUPLICATE = "listing.mark-unallocated-hearing-as-duplicate";
    private static final String ACTION_DELETE_HEARING = "listing.command.delete-hearing";



    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToListCourtHearing() {
        final Action action = createActionFor(ACTION_LIST_COURT_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, LEGAL_ADVISERS, COURT_CLERKS, SYSTEM_USERS, COURT_ASSOCIATE, MAGISTRATES))
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
    public void shouldAllowAuthorisedUserToListUnscheduledCourtHearing() {
        final Action action = createActionFor(ACTION_LIST_UNSCHEDULED_COURT_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, LEGAL_ADVISERS, COURT_CLERKS, SYSTEM_USERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToListUnscheduledCourtHearing() {
        final Action action = createActionFor(ACTION_LIST_UNSCHEDULED_COURT_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, COURT_ADMINISTRATORS, COURT_CLERKS, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToUpdateHearingForListing() {
        final Action action = createActionFor(ACTION_UPDATE_HEARING_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToChangeJudiciaryForHearing() {
        final Action action = createActionFor(ACTION_CHANGE_JUDICIARY_FOR_HEARING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToSequenceHearings() {
        final Action action = createActionFor(ACTION_SEQUENCE_HEARINGS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, CPS, YOTS, NPS, COURT_ASSOCIATE, GROUP_POLICE_ADMIN, GROUP_VICTIMS_WITNESS_CARE_ADMIN, NON_CPS_PROSECUTOR_GROUP))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToRestrictCourtList() {
        final Action action = createActionFor(ACTION_RESTRICT_COURT_LIST);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS, CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, CPS, YOTS, NPS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }


    @Test
    public void shouldAllowAuthorisedUserToPublishCourtList() {
        final Action action = createActionFor(ACTION_PUBLISH_COURT_LIST);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToCreateListingNote() {
        final Action action = createActionFor(ACTION_CREATE_LISTING_NOTE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToCreateListingNote() {
        final Action action = createActionFor(ACTION_CREATE_LISTING_NOTE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Test
    public void shouldAllowAuthorisedUserToDeleteListingNote() {
        final Action action = createActionFor(ACTION_DELETE_LISTING_NOTE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, LISTING_OFFICERS,
                CROWN_COURT_ADMIN, COURT_ADMINISTRATORS, COURT_CLERKS, LEGAL_ADVISERS, COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToDeleteListingNote() {
        final Action action = createActionFor(ACTION_DELETE_LISTING_NOTE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RANDOM_GROUP)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserToPublishCourtListsForCrownCourts() {
        final Action action = createActionFor(ACTION_PUBLISH_COURT_LISTS_FOR_CROWN_COURTS);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNonSystemUserToPublishCourtListsForCrownCourts() {
        final Action action = createActionFor(ACTION_PUBLISH_COURT_LISTS_FOR_CROWN_COURTS);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserToCourtListRequestExport() {
        final Action action = createActionFor(ACTION_COURT_LIST_REQUEST_EXPORT);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNonSystemUserToCourtListRequestExport() {
        final Action action = createActionFor(ACTION_COURT_LIST_REQUEST_EXPORT);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserToMarkUnallocatedHearingAsDuplicate() {
        final Action action = createActionFor(ACTION_MARK_UNALLOCATED_HEARING_AS_DUPLICATE);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNonSystemUserToMarkUnallocatedHearingAsDuplicate() {
        final Action action = createActionFor(ACTION_MARK_UNALLOCATED_HEARING_AS_DUPLICATE);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldOnlyAllowHMCTSusersToEditNote() {

        final Action action = createActionFor(ACTION_EDIT_NOTE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                RuleConstants.LISTING_OFFICERS, RuleConstants.CROWN_COURT_ADMIN,
                RuleConstants.COURT_ADMINISTRATORS, RuleConstants.COURT_CLERKS,
                RuleConstants.LEGAL_ADVISERS, RuleConstants.COURT_ASSOCIATE))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUsersToEditNote() {

        final Action action = createActionFor(ACTION_EDIT_NOTE_FOR_LISTING);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                RuleConstants.YOTS, CPS,
                NPS))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserToDeleteHearing() {
        final Action action = createActionFor(ACTION_DELETE_HEARING);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNonSystemUserToDeleteHearing() {
        final Action action = createActionFor(ACTION_DELETE_HEARING);
        given(userAndGroupProvider.isSystemUser(action)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
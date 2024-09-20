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

public class UpdateHearingForListingAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UpdateHearingForListingAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
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
    public void shouldNotAllowUnauthorisedUserToUpdateHearingForListing() {
        final Action action = createActionFor(ACTION_UPDATE_HEARING_FOR_LISTING);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
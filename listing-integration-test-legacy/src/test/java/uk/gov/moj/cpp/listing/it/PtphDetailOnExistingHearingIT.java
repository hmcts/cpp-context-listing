package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.trialHearingsData;
import static uk.gov.moj.cpp.listing.utils.HearingServiceStub.stubFinalisedPtphDetail;
import static uk.gov.moj.cpp.listing.utils.HearingServiceStub.stubNotFinalisedPtphDetail;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataTrialHearingTypes;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.text.MessageFormat;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/**
 * LPT-2405 — the next hearing already exists, so nothing is created and no
 * {@code listing.list-next-hearings-v2} is ever sent. Progression skips the creation path
 * entirely when a result carries an {@code existingHearingId} prompt, and instead raises
 * {@code listing.update-related-hearing} for the hearing that already exists.
 *
 * <p>That command carries the seeding hearing but no hearing type, so the command API looks the
 * stored hearing up ({@code HearingLookupService}) and feeds its jurisdiction and type into the
 * same gates as the create flows. These tests pin the two outcomes that matter: a Crown Court
 * trial inherits, and anything else does not.
 *
 * <p>The values cannot arrive on a {@code hearing-listed} event here — the row already exists —
 * so they are patched into {@code hearing.properties} by the cases-added listener instead.
 */
class PtphDetailOnExistingHearingIT extends AbstractIT {

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    private static final String TIER = "TIER_3";
    private static final String LIST_TYPE = "TYPE_1_FIXED";
    private static final String KEY_REASON = "Vulnerable witness";

    /** The hearing type id {@code trialHearingsData()} builds its hearings with. */
    private static final UUID TRIAL_HEARING_TYPE_ID = UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4");

    @Test
    void shouldInheritTierAndListTypeOntoAnExistingCrownTrialHearing() {
        stubFinalisedPtphDetail(TIER, LIST_TYPE, KEY_REASON);

        final HearingsData seedingHearings = hearingsData();
        final ListCourtHearingSteps seedingSteps = new ListCourtHearingSteps(seedingHearings);
        seedingSteps.whenCaseIsSubmittedForListing();
        seedingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        // the next hearing already exists, and is a Crown Court trial
        final HearingsData existingHearings = trialHearingsData();
        final ListCourtHearingSteps existingSteps = new ListCourtHearingSteps(existingHearings);
        existingSteps.whenCaseIsSubmittedForListing();
        existingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID existingHearingId = existingHearings.getHearingData().get(0).getId();

        // registered at priority 1, so it beats the stub the steps class registers for the same url
        stubGetReferenceDataTrialHearingTypes(TRIAL_HEARING_TYPE_ID);

        new ListNextHearingSteps(seedingHearings.getHearingData().get(0))
                .whenUpdateRelatedHearingSubmittedForListing(existingHearingId, hearingsData());

        // prove the hearing is in the response before asserting on its fields, so these
        // assertions cannot pass vacuously against an empty filter result
        pollForHearingProperty(existingHearingId, "id", contains(existingHearingId.toString()));
        pollForHearingProperty(existingHearingId, "tier", contains(TIER));
        pollForHearingProperty(existingHearingId, "listType", contains(LIST_TYPE));
        pollForHearingProperty(existingHearingId, "keyReason", contains(KEY_REASON));
    }

    /**
     * Tier and list type are a Crown Court PTPH concern. An existing hearing that is not a trial
     * must be left alone — and the hearing context must not even be asked.
     */
    @Test
    void shouldNotInheritWhenTheExistingHearingIsNotATrial() {
        stubFinalisedPtphDetail(TIER, LIST_TYPE, KEY_REASON);

        final HearingsData seedingHearings = hearingsData();
        final ListCourtHearingSteps seedingSteps = new ListCourtHearingSteps(seedingHearings);
        seedingSteps.whenCaseIsSubmittedForListing();
        seedingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        // a plain (non-trial) existing hearing
        final HearingsData existingHearings = hearingsData();
        final ListCourtHearingSteps existingSteps = new ListCourtHearingSteps(existingHearings);
        existingSteps.whenCaseIsSubmittedForListing();
        existingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID existingHearingId = existingHearings.getHearingData().get(0).getId();

        stubGetReferenceDataTrialHearingTypes(TRIAL_HEARING_TYPE_ID);

        new ListNextHearingSteps(seedingHearings.getHearingData().get(0))
                .whenUpdateRelatedHearingSubmittedForListing(existingHearingId, hearingsData());

        pollForHearingProperty(existingHearingId, "id", contains(existingHearingId.toString()));
        pollForHearingProperty(existingHearingId, "tier", empty());
        pollForHearingProperty(existingHearingId, "listType", empty());
    }

    /**
     * A draft PTPH record must not leak onto the trial: only a finalised one is inherited.
     */
    @Test
    void shouldNotInheritWhenTheSeedingRecordIsNotFinalised() {
        stubNotFinalisedPtphDetail(TIER, LIST_TYPE);

        final HearingsData seedingHearings = hearingsData();
        final ListCourtHearingSteps seedingSteps = new ListCourtHearingSteps(seedingHearings);
        seedingSteps.whenCaseIsSubmittedForListing();
        seedingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final HearingsData existingHearings = trialHearingsData();
        final ListCourtHearingSteps existingSteps = new ListCourtHearingSteps(existingHearings);
        existingSteps.whenCaseIsSubmittedForListing();
        existingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID existingHearingId = existingHearings.getHearingData().get(0).getId();

        stubGetReferenceDataTrialHearingTypes(TRIAL_HEARING_TYPE_ID);

        new ListNextHearingSteps(seedingHearings.getHearingData().get(0))
                .whenUpdateRelatedHearingSubmittedForListing(existingHearingId, hearingsData());

        pollForHearingProperty(existingHearingId, "id", contains(existingHearingId.toString()));
        pollForHearingProperty(existingHearingId, "tier", empty());
        pollForHearingProperty(existingHearingId, "listType", empty());
    }

    /**
     * A filtered JsonPath yields a list, so the matchers above are collection matchers:
     * {@code contains(...)} for a present value and {@code empty()} for an absent one.
     */
    private void pollForHearingProperty(final UUID hearingId, final String property, final Matcher<?> matcher) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), UNALLOCATED));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON)
                .withHeader(USER_ID, getLoggedInUser()))
                .until(payload().isJson(withJsonPath(
                        String.format("$.hearings[?(@.id=='%s')].%s", hearingId, property), matcher)));
    }
}

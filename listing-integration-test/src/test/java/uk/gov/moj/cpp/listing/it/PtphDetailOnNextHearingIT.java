package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.HearingServiceStub.stubFinalisedPtphDetail;
import static uk.gov.moj.cpp.listing.utils.HearingServiceStub.stubNotFinalisedPtphDetail;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataTrialHearingTypes;

import uk.gov.moj.cpp.listing.steps.PayloadBasedListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadBasedListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadGenerator;

import java.text.MessageFormat;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LPT-2405 — a trial next hearing listed from a seeding hearing inherits that hearing's
 * finalised tier / list type / key reason into the listing view store.
 *
 * <p>Two stubs are load-bearing here, and the test is meaningless without both:
 *
 * <ul>
 *   <li>{@link uk.gov.moj.cpp.listing.utils.HearingServiceStub} serves the hearing context's
 *       {@code hearing.get-ptph-detail} query. Listing reaches it through a REST client
 *       generated from {@code hearing-query-api}'s RAML, wired in
 *       {@code listing-command-api/pom.xml} — without that plugin dependency there is no
 *       {@code @Handles("hearing.get-ptph-detail")} and the request cannot dispatch.</li>
 *   <li>{@code stubGetReferenceDataTrialHearingTypes} serves the next hearing's type with
 *       {@code trialTypeFlag: true}. The shared referencedata stub carries no such flag, so
 *       without this the trial gate never opens, the hearing context is never queried and
 *       nothing is inherited — the positive test would fail and the negative one would pass
 *       for entirely the wrong reason.</li>
 * </ul>
 *
 * <p>The enrichment rules themselves are covered without the remote hop by
 * {@code PtphDetailEnrichmentServiceTest} and {@code PtphDetailServiceTest}.
 */
class PtphDetailOnNextHearingIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailOnNextHearingIT.class);

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    private static final String TIER = "TIER_3";
    private static final String LIST_TYPE = "TYPE_1_FIXED";
    private static final String KEY_REASON = "Vulnerable witness";

    /**
     * The hearing type id carried by the next hearing in
     * {@code test-data/CROWN/list-next-hearings-v2/adjorunment_crown_fixed_date.json}. The trial
     * gate compares the command payload's {@code type.id} against the reference-data ids
     * flagged {@code trialTypeFlag}, so the stub must serve this id — not the unrelated id the
     * steps class stubs for hearing durations.
     */
    private static final UUID NEXT_HEARING_TYPE_ID = UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4");

    @Test
    void shouldInheritTierAndListTypeWhenSeedingRecordIsFinalised() {
        stubFinalisedPtphDetail(TIER, LIST_TYPE, KEY_REASON);

        final PayloadBasedListCourtHearingSteps seedingHearingSteps = new PayloadBasedListCourtHearingSteps();
        final PayloadGenerator.PayloadValues seedingHearing = seedingHearingSteps.whenListCourtHearingSubmittedWithAdhocHearingCreation();
        seedingHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        final PayloadBasedListNextHearingSteps nextHearingSteps = new PayloadBasedListNextHearingSteps(seedingHearing.hearingId);
        // registered at priority 1 so it beats the steps class stub for the same URL,
        // which is registered later (inside the when... method below)
        stubGetReferenceDataTrialHearingTypes(NEXT_HEARING_TYPE_ID);
        final PayloadGenerator.PayloadValues nextHearing = nextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();
        nextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED, 2);

        // The listener serialises the whole event hearing into hearing.properties (jsonb),
        // so the inherited values surface on the search response for the new hearing.
        // prove the hearing is actually in the response first, so the assertions below
        // cannot pass vacuously by matching an empty filter result
        pollForHearingProperty(nextHearing.hearingId, "id", contains(nextHearing.hearingId));
        pollForHearingProperty(nextHearing.hearingId, "tier", contains(TIER));
        pollForHearingProperty(nextHearing.hearingId, "listType", contains(LIST_TYPE));
        pollForHearingProperty(nextHearing.hearingId, "keyReason", contains(KEY_REASON));

        LOGGER.info("Next hearing {} inherited tier {} from seeding hearing {}",
                nextHearing.hearingId, TIER, seedingHearing.hearingId);
    }

    @Test
    void shouldNotInheritWhenSeedingRecordIsNotFinalised() {
        stubNotFinalisedPtphDetail(TIER, LIST_TYPE);

        final PayloadBasedListCourtHearingSteps seedingHearingSteps = new PayloadBasedListCourtHearingSteps();
        final PayloadGenerator.PayloadValues seedingHearing = seedingHearingSteps.whenListCourtHearingSubmittedWithAdhocHearingCreation();
        seedingHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        final PayloadBasedListNextHearingSteps nextHearingSteps = new PayloadBasedListNextHearingSteps(seedingHearing.hearingId);
        // registered at priority 1 so it beats the steps class stub for the same URL,
        // which is registered later (inside the when... method below)
        stubGetReferenceDataTrialHearingTypes(NEXT_HEARING_TYPE_ID);
        final PayloadGenerator.PayloadValues nextHearing = nextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();
        nextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED, 2);

        // the hearing must exist but carry no tier — assert presence before absence, or an
        // empty filter result would make this pass for the wrong reason
        pollForHearingProperty(nextHearing.hearingId, "id", contains(nextHearing.hearingId));
        pollForHearingProperty(nextHearing.hearingId, "tier", empty());
        pollForHearingProperty(nextHearing.hearingId, "listType", empty());

        LOGGER.info("Next hearing {} correctly inherited nothing from a non-finalised seeding record",
                nextHearing.hearingId);
    }

    private void pollForHearingProperty(final String hearingId, final String property, final Matcher<?> matcher) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), AbstractIT.ALLOCATED));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON)
                .withHeader(USER_ID, getLoggedInUser()))
                .until(payload().isJson(withJsonPath(
                        String.format("$.hearings[?(@.id=='%s')].%s", hearingId, property), matcher)));
    }
}

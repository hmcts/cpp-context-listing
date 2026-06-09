package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.UUID;

import javax.json.JsonObject;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendantLegalAidStatusUpdateSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantLegalAidStatusUpdateSteps.class);

    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";

    private JmsMessageProducerClient publicEventDefendantLegalAidStatusUpdated;


    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;

    public DefendantLegalAidStatusUpdateSteps(UUID caseId, HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);

        publicEventDefendantLegalAidStatusUpdated = QueueUtil.publicEvents.createPublicProducer();

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished() {
        // Fresh randomUUID() per publish: the framework dedupes events by metadata id, so
        // re-publishing with the same id would be ignored. A new id guarantees each re-publish
        // is reprocessed by the aggregate.
        final JsonObject payload = getPayloadForPublicEventFromHearingData();
        sendMessage(
                publicEventDefendantLegalAidStatusUpdated,
                PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED,
                payload,
                metadataOf(randomUUID(), PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).withUserId(randomUUID().toString()).build());
    }

    /**
     * Asserts that the read model reflects legalAidStatus == "Granted" for the hearing.
     * Uses JMS-aware polling to handle asynchronous message processing.
     */
    public void verifyLegalAidStatusGranted() {
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].legalAidStatus", equalTo("Granted"))
        });
    }

    /**
     * Publishes the {@code public.progression.defendant-legalaid-status-updated} event and verifies
     * the read model, RE-PUBLISHING until the update is reflected (or the attempt budget is exhausted).
     *
     * <p><b>Why re-publish instead of publish-once-then-poll?</b> The event is consumed by
     * {@code ListingEventProcessor} and routes to the {@code Case} aggregate. That aggregate
     * <b>silently drops the update</b> ({@code if (hearingIds.isEmpty()) return Stream.empty();})
     * when the case is not yet linked to a hearing. {@code Case.hearingIds} is populated only after
     * the asynchronous {@code add-hearing-to-case} command runs — and there is no viewstore
     * projection to await that link. On slow CI the first publish can arrive before the link exists,
     * is dropped with no JMS redelivery, and the 90s poll can never succeed. Re-publishing with a
     * fresh metadata id each attempt guarantees that once the link forms, a subsequent publish lands.
     */
    public void publishUntilLegalAidStatusReflected() {
        final int maxPublishAttempts = 3;
        for (int attempt = 1; attempt <= maxPublishAttempts; attempt++) {
            LOGGER.info("[legalaid-fix] publishing defendant-legalaid-status-updated for case {} (attempt {}/{})",
                    caseId, attempt, maxPublishAttempts);
            whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished();
            try {
                verifyLegalAidStatusGranted();
                LOGGER.info("[legalaid-fix] read model reflected legalAidStatus update after {} publish attempt(s)", attempt);
                return;
            } catch (final ConditionTimeoutException caseNotYetLinkedToHearing) {
                if (attempt == maxPublishAttempts) {
                    LOGGER.error("[legalaid-fix] update still not reflected after {} attempts — failing", maxPublishAttempts);
                    throw caseNotYetLinkedToHearing;
                }
                // The case<->hearing link was not established when this publish was processed, so the
                // update was dropped. Re-publish with a fresh event id and poll again.
                LOGGER.warn("[legalaid-fix] attempt {} did not land (case<->hearing link likely not yet established); re-publishing", attempt);
            }
        }
    }

    private JsonObject getPayloadForPublicEventFromHearingData() {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", listedCaseData.getDefendants().get(0).getDefendantId().toString())
                .add("legalAidStatus", "Granted")
                .build();
    }
}

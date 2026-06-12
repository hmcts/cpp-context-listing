package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.it.util.PublishRetryHelper.publishUntilReflected;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseUpdatedAndDefendantProceedingsConcludedSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseUpdatedAndDefendantProceedingsConcludedSteps.class);

    private static final String PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";

    private JmsMessageProducerClient publicEventCaseUpdatedAndHearingResulted;

    private final UUID userId;
    private final UUID caseId;
    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;

    public CaseUpdatedAndDefendantProceedingsConcludedSteps(UUID caseId, HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.userId = randomUUID();
        this.listedCaseData = hearingData.getListedCases().get(0);

        this.publicEventCaseUpdatedAndHearingResulted = publicEvents.createPublicProducer();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenPublicEventCaseUpdatedAndHearingResultedIsPublished() {
        final UUID defendantId = listedCaseData.getDefendants().get(0).getDefendantId();
        final String eventPayloadString = getPayload("public.progression.hearing-resulted-case-updated.json")
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        // Fresh metadata id per publish: the framework dedupes events by metadata id, so re-publishing
        // with the same id would be silently ignored. A new id guarantees each re-publish is reprocessed.
        sendMessage(publicEventCaseUpdatedAndHearingResulted,
                PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED)
                        .withUserId(userId.toString())
                        .build());

    }

    /**
     * Publishes the {@code public.progression.hearing-resulted-case-updated} event and verifies the read model,
     * RE-PUBLISHING until the update is reflected (or the attempt budget is exhausted).
     *
     * <p><b>Why re-publish instead of publish-once-then-poll?</b> The event is consumed by
     * {@code ListingEventProcessor} and routed to the {@code Case} aggregate's handler. That handler
     * <b>silently drops the update</b> ({@code if (hearingIds.isEmpty()) return Stream.empty();}) when the
     * aggregate does not yet know which hearing the case belongs to. {@code Case.hearingIds} is populated only
     * after the asynchronous {@code add-hearing-to-case} command runs — itself triggered by a private event
     * emitted after {@code list-court-hearing} — and there is <b>no viewstore projection</b> for the link, so
     * the test cannot deterministically await it. On slower CI environments the first publish can be processed
     * before the link exists; it is then dropped with no JMS redelivery, so a single publish is lost forever.
     * Re-publishing (with a fresh event id each time) guarantees that once the link is established, a
     * subsequent publish lands.
     */
    public void publishUntilCaseStatusReflected(final boolean isAllocated) {
        publishUntilReflected(LOGGER, "defendant-proceeding-fix", "hearing-resulted-case-updated",
                this::whenPublicEventCaseUpdatedAndHearingResultedIsPublished,
                () -> verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPIWithJmsDelay(isAllocated));
    }

    public void verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI(boolean isAllocated) {
        pollForHearing(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].proceedingsConcluded",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].caseStatus",
                        equalTo("CLOSED"))
        });
    }

    /**
     * JMS-aware version of verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI for handling asynchronous message processing timing issues.
     */
    public void verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPIWithJmsDelay(boolean isAllocated) {
        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].proceedingsConcluded",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].caseStatus",
                        equalTo("CLOSED"))
        });
    }

    public void verifyHearingForCaseStatusAndDefendantProceedingsConcludedNotSetFromAPI(boolean isAllocated) {
        pollForHearing(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withoutJsonPath("$.hearings[0].listedCases[0].defendants[0].proceedingsConcluded"),
                withoutJsonPath("$.hearings[0].listedCases[0].caseStatus")
        });
    }

    /**
     * JMS-aware version of verifyHearingForCaseStatusAndDefendantProceedingsConcludedNotSetFromAPI for handling asynchronous message processing timing issues.
     */
    public void verifyHearingForCaseStatusAndDefendantProceedingsConcludedNotSetFromAPIWithJmsDelay(boolean isAllocated) {
        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withoutJsonPath("$.hearings[0].listedCases[0].defendants[0].proceedingsConcluded"),
                withoutJsonPath("$.hearings[0].listedCases[0].caseStatus")
        });
    }
}

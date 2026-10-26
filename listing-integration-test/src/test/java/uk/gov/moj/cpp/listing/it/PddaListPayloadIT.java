package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.progression.courts.CaseLinked.caseLinked;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.util.PublishRetryHelper.publishUntilReflected;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.progression.courts.CaseLinked;
import uk.gov.justice.progression.courts.Cases;
import uk.gov.justice.progression.courts.LinkActionType;
import uk.gov.justice.progression.courts.LinkedToCases;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PddaListPayloadSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@code application/vnd.listing.search.crown.list.payload+json}, requested from the daily list
 * resource: the daily list plus linked cases, sitting notes, judge name parts and further court centre address details.
 */
public class PddaListPayloadIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PddaListPayloadIT.class);

    private static final String PUBLIC_EVENT_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    private static final UUID LINKED_CASE_ID_1 = randomUUID();
    private static final String LINKED_CASE_URN_1 = STRING.next();
    private static final UUID LINKED_CASE_ID_2 = randomUUID();
    private static final String LINKED_CASE_URN_2 = STRING.next();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    private PddaListPayloadSteps pddaListPayloadSteps;
    private HearingsData hearingsData;

    @BeforeEach
    public void setUp() {
        super.setUp();
        hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final UpdatedHearingData updatedHearingData = updatedHearingDataForCrownAllocation(hearingsData.getHearingData().get(0).getId());

        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearingsData.getHearingData().get(0).getId().toString(),
                updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                updatedHearingData.getNonDefaultDays().get(1).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingData.getNonDefaultDays().get(0).getStartTime()),
                updatedHearingData.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        pddaListPayloadSteps = new PddaListPayloadSteps(hearingsData, updatedHearingData);
    }

    @Test
    void shouldReturnTheDailyListDataAndJudiciaryNamePartsForDraft() {
        pddaListPayloadSteps.verifyPddaListContainsTheDailyListDataAndJudiciaryNameParts("DRAFT");
    }

    @Test
    void shouldReturnTheDailyListDataAndJudiciaryNamePartsForFinal() {
        pddaListPayloadSteps.verifyPddaListContainsTheDailyListDataAndJudiciaryNameParts("FINAL");
    }

    @Test
    void shouldReturnCourtCentreAddressDetails() {
        pddaListPayloadSteps.verifyPddaListContainsCourtCentreAddressDetails("DRAFT");
    }

    @Test
    void shouldReturnTheSittingNoteForTheSittingsCourtRoomAndDate() {
        final String note = "Court sits late today " + STRING.next();

        pddaListPayloadSteps.createSittingNote(note);

        pddaListPayloadSteps.verifyPddaListContainsSittingNote("DRAFT", note);
        pddaListPayloadSteps.verifyDailyListHasNoAdditionalData("DRAFT");
    }

    @Test
    void shouldReturnLinkedCasesAndLeaveThemOutOfTheDailyList() {
        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final String caseUrn = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseReference();
        final JmsMessageProducerClient publicEventProgressionCaseLinked = publicEvents.createPublicProducer();

        // The case aggregate drops the link when it does not yet know the hearing, so re-publish until it is reflected
        publishUntilReflected(LOGGER, "pdda-linked-cases", "cases-linked event for hearing " + hearingId,
                () -> publishCasesLinkedEvent(publicEventProgressionCaseLinked, createCaseLinkedEvent(caseId, caseUrn)),
                () -> pddaListPayloadSteps.verifyPddaListContainsLinkedCases("DRAFT", LINKED_CASE_URN_1, LINKED_CASE_URN_2));

        pddaListPayloadSteps.verifyDailyListHasNoAdditionalData("DRAFT");
    }

    @Test
    void shouldLeaveTheAdditionalDataOutOfTheDailyListPayload() {
        pddaListPayloadSteps.verifyDailyListHasNoAdditionalData("DRAFT");
    }

    private CaseLinked createCaseLinkedEvent(final UUID caseId, final String caseUrn) {
        return caseLinked()
                .withLinkActionType(LinkActionType.LINK)
                .withCases(Arrays.asList(
                        Cases.cases()
                                .withCaseId(caseId)
                                .withCaseUrn(caseUrn)
                                .withLinkedToCases(Arrays.asList(
                                        LinkedToCases.linkedToCases().withCaseId(LINKED_CASE_ID_1).withCaseUrn(LINKED_CASE_URN_1).build(),
                                        LinkedToCases.linkedToCases().withCaseId(LINKED_CASE_ID_2).withCaseUrn(LINKED_CASE_URN_2).build()))
                                .build()))
                .build();
    }

    private void publishCasesLinkedEvent(final JmsMessageProducerClient producer, final CaseLinked caseLinked) {
        final JsonObject caseLinkedObject = (JsonObject) objectToJsonValueConverter.convert(caseLinked);

        sendMessage(
                producer,
                PUBLIC_EVENT_PROGRESSION_CASE_LINKED,
                caseLinkedObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_PROGRESSION_CASE_LINKED).withUserId(randomUUID().toString()).build());
    }
}

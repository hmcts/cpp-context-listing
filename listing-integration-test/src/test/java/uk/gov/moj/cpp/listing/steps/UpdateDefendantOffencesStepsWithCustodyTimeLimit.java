package uk.gov.moj.cpp.listing.steps;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantUpdated;
import uk.gov.justice.progression.courts.UpdatedOffences;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceDataWithCustodyTimeLimit;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.path.json.JsonPath;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateDefendantOffencesStepsWithCustodyTimeLimit extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantOffencesStepsWithCustodyTimeLimit.class);


    private static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";

    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";

    private static final String EVENT_SELECTOR_OFFENCES_UPDATED = "listing.events.offence-updated";
    private static final String EVENT_SELECTOR_OFFENCES_ADDED = "listing.events.offence-added";
    private static final String EVENT_SELECTOR_OFFENCES_DELETED = "listing.events.offence-deleted";


    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";


    JSONComparator ignoreMetaDataComparator = new CustomComparator(JSONCompareMode.LENIENT, new Customization("_metadata", (o1, o2) -> true));


    private MessageProducer publicEventDefendantOffencesUpdated;
    private MessageConsumer publicEventMessageConsumerDefendantOffencesUpdated;

    private MessageConsumer privateEventMessageOffencesToBeUpdated;
    private MessageConsumer privateEventMessageOffencesToBeDeleted;
    private MessageConsumer privateEventMessageOffencesToBeAdded;

    private MessageConsumer privateEventsMessageOffenceUpdated;
    private MessageConsumer privateEventsMessageOffenceDeleted;
    private MessageConsumer privateEventsMessageOffenceAdded;


    private String request;


    private final HearingData hearingData;
    private final UpdatedOffenceDataWithCustodyTimeLimit updatedOffenceData;
    private final ListedCaseData listedCaseData;
    private final DefendantData defendantData;
    private final OffenceData offenceData;
    private final UUID offenceIdToBeDeleted;
    private final UUID caseId;
    private final UUID metadataId;
    private final UUID userId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);


    public UpdateDefendantOffencesStepsWithCustodyTimeLimit(UUID caseId, HearingData hearingData, UpdatedOffenceDataWithCustodyTimeLimit updatedOffenceData, UUID offenceIdToBeDeleted) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);
        this.defendantData = listedCaseData.getDefendants().get(0);
        this.offenceData = defendantData.getOffences().get(0);
        this.updatedOffenceData = updatedOffenceData;
        this.metadataId = randomUUID();
        this.userId = randomUUID();
        this.offenceIdToBeDeleted = offenceIdToBeDeleted;


        publicEventDefendantOffencesUpdated = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerDefendantOffencesUpdated = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED);

        privateEventMessageOffencesToBeUpdated = privateEvents.createConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED);
        privateEventMessageOffencesToBeDeleted = privateEvents.createConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_DELETED);
        privateEventMessageOffencesToBeAdded = privateEvents.createConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_ADDED);

        privateEventsMessageOffenceUpdated = privateEvents.createConsumer(EVENT_SELECTOR_OFFENCES_UPDATED);
        privateEventsMessageOffenceAdded = privateEvents.createConsumer(EVENT_SELECTOR_OFFENCES_ADDED);
        privateEventsMessageOffenceDeleted = privateEvents.createConsumer(EVENT_SELECTOR_OFFENCES_DELETED);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    public void whenCaseDefendantOffencesUpdatedPublicEventIsPublished() {
        OffencesForDefendantUpdated offencesForDefendantUpdated = getOffencesForDefendantUpdated(caseId, defendantData.getDefendantId());
        publishCaseDefendantOffencesUpdated(offencesForDefendantUpdated);
    }

    private void publishCaseDefendantOffencesUpdated(OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final JsonObject updateCaseDefendantDetailsObject = (JsonObject) objectToJsonValueConverter.convert(offencesForDefendantUpdated);

        QueueUtil.sendMessage(
                publicEventDefendantOffencesUpdated,
                PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED,
                updateCaseDefendantDetailsObject,
                metadataOf(metadataId, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED).withUserId(userId.toString()).build());

        request = updateCaseDefendantDetailsObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, request, getLoggedInHeader());
    }


    public void verifyPublicEventDefendantOffencesUpdatedInActiveMQ() throws Exception {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(publicEventMessageConsumerDefendantOffencesUpdated);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerDefendantOffencesUpdated: {}", jsonResponse);

        String expected =
                "{\n" +
                        "  \"addedOffences\": [\n" +
                        "    {\n" +
                        "      \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "      \"offences\": [\n" +
                        "        {\n" +
                        "          \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                        "          \"id\": \"" + updatedOffenceData.getRandomOffenceId() + "\",\n" +
                        "          \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() + "\",\n" +
                        "          \"offenceLegislation\": \"" + updatedOffenceData.getLegislation() + "\",\n" +
                        "          \"offenceLegislationWelsh\": \"" + updatedOffenceData.getLegislationWelsh() + "\",\n" +
                        "          \"offenceTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitle() + "\",\n" +
                        "          \"offenceTitleWelsh\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\",\n" +
                        "          \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "          \"count\": " + offenceData.getCount() + ",\n" +
                        "          \"offenceDefinitionId\": \"" + offenceData.getOffenceDefinitionId() + "\",\n" +
                        "          \"wording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "    \"custodyTimeLimit\": {\n" +
                        "      \"timeLimit\": \"2020-01-06\",\n" +
                        "      \"daysSpent\":1 \n" +
                        "      }\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"prosecutionCaseId\": \"" + caseId + "\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"deletedOffences\": [\n" +
                        "    {\n" +
                        "      \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "      \"offences\": [\n" +
                        "        \"" + offenceIdToBeDeleted + "\"\n" +
                        "      ],\n" +
                        "      \"prosecutionCaseId\": \"" + caseId + "\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"modifiedDate\": \"" + LocalDate.now().toString() + "\",\n" +
                        "  \"updatedOffences\": [\n" +
                        "    {\n" +
                        "      \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "      \"offences\": [\n" +
                        "        {\n" +
                        "          \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                        "          \"id\": \"" + updatedOffenceData.getOffenceId() + "\",\n" +
                        "          \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() + "\",\n" +
                        "          \"offenceLegislation\": \"" + updatedOffenceData.getLegislation() + "\",\n" +
                        "          \"offenceLegislationWelsh\": \"" + updatedOffenceData.getLegislationWelsh() + "\",\n" +
                        "          \"offenceTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitle() + "\",\n" +
                        "          \"offenceTitleWelsh\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\",\n" +
                        "          \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "          \"count\": " + offenceData.getCount() + ",\n" +
                        "          \"offenceDefinitionId\": \"" + offenceData.getOffenceDefinitionId() + "\",\n" +
                        "          \"wording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "    \"custodyTimeLimit\": {\n" +
                        "      \"timeLimit\": \"2020-01-06\",\n" +
                        "      \"daysSpent\":1 \n" +
                        "      }\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"prosecutionCaseId\": \"" + caseId + "\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"_metadata\": {\n" +
                        "    \"name\": \"" + PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED + "\",\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"" + userId + "\"\n" +
                        "    },\n" +
                        "    \"id\": \"" + metadataId + "\"\n" +
                        "  }\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, true);
    }


    private OffencesForDefendantUpdated getOffencesForDefendantUpdated(UUID caseId, UUID defendantId) {

        return OffencesForDefendantUpdated.offencesForDefendantUpdated()
                .withAddedOffences(buildAddedOffences(caseId, defendantId))
                .withDeletedOffences(buildDeletedOffence(caseId, defendantId, offenceIdToBeDeleted))
                .withModifiedDate(LocalDate.now().toString())
                .withUpdatedOffences(buildUpdatedOffences(caseId, defendantId, updatedOffenceData))
                .build();
    }


    private List<DeletedOffences> buildDeletedOffence(final UUID caseId, final UUID defendantId, final UUID offenceIdToBeDeleted) {
        DeletedOffences deletedOffences = DeletedOffences.deletedOffences()
                .withProsecutionCaseId(caseId)
                .withDefendantId(defendantId)
                .withOffences(singletonList(offenceIdToBeDeleted))
                .build();
        return singletonList(deletedOffences);
    }

    private List<UpdatedOffences> buildUpdatedOffences(UUID caseId, UUID defendantId, UpdatedOffenceDataWithCustodyTimeLimit updatedOffenceData) {
        UpdatedOffences updatedOffences = UpdatedOffences.updatedOffences()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(buildOffence(updatedOffenceData.getOffenceId())))
                .build();
        return singletonList(updatedOffences);
    }

    private List<AddedOffences> buildAddedOffences(UUID caseId, UUID defendantId) {
        AddedOffences addedOffences = AddedOffences.addedOffences()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(buildOffence(updatedOffenceData.getRandomOffenceId())))
                .build();
        return singletonList(addedOffences);
    }

    private Offence buildOffence(UUID offenceId) {
        return Offence.offence()
                .withId(offenceId)
                .withOffenceCode(updatedOffenceData.getOffenceCode())
                .withStartDate(updatedOffenceData.getStartDate().toString())
                .withWording(updatedOffenceData.getOffenceWording())
                .withEndDate(of(updatedOffenceData.getEndDate().toString()))
                .withOffenceTitle(updatedOffenceData.getStatementOfOffenceTitle())
                .withOffenceTitleWelsh(of(updatedOffenceData.getStatementOfOffenceTitleWelsh()))
                .withWording(updatedOffenceData.getOffenceWording())
                .withOffenceLegislation(of(updatedOffenceData.getLegislation()))
                .withOffenceLegislationWelsh(of(updatedOffenceData.getLegislationWelsh()))
                .withCount(Optional.of(offenceData.getCount()))
                .withOffenceDefinitionId(offenceData.getOffenceDefinitionId())
                .withCustodyTimeLimit(offenceData.getCustodyTimeLimit())
                .build();
    }


    @Override
    public void close() {
        try {
            publicEventDefendantOffencesUpdated.close();
            publicEventMessageConsumerDefendantOffencesUpdated.close();

            privateEventMessageOffencesToBeUpdated.close();
            privateEventMessageOffencesToBeDeleted.close();
            privateEventMessageOffencesToBeAdded.close();

            privateEventsMessageOffenceUpdated.close();
            privateEventsMessageOffenceDeleted.close();
            privateEventsMessageOffenceAdded.close();
        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}

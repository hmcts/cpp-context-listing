package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.core.courts.LaaReference.laaReference;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessageString;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantUpdated;
import uk.gov.justice.progression.courts.UpdatedOffences;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.LaaReferenceData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.ReportingRestrictionData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateDefendantOffencesSteps extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantOffencesSteps.class);

    private static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";

    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";

    private static final String EVENT_SELECTOR_OFFENCES_UPDATED = "listing.events.offence-updated";
    private static final String EVENT_SELECTOR_OFFENCES_ADDED = "listing.events.offence-added";
    private static final String EVENT_SELECTOR_OFFENCES_DELETED = "listing.events.offence-deleted";


    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    JSONComparator ignoreMetaDataComparator = new CustomComparator(JSONCompareMode.LENIENT, new Customization("_metadata", (o1, o2) -> true));

    private JmsMessageProducerClient publicEventDefendantOffencesUpdated;
    private JmsMessageConsumerClient publicEventMessageConsumerDefendantOffencesUpdated;

    private JmsMessageConsumerClient privateEventMessageOffencesToBeUpdated;
    private JmsMessageConsumerClient privateEventMessageOffencesToBeDeleted;
    private JmsMessageConsumerClient privateEventMessageOffencesToBeAdded;

    private JmsMessageConsumerClient privateEventsMessageOffenceUpdated;
    private JmsMessageConsumerClient privateEventsMessageOffenceDeleted;
    private JmsMessageConsumerClient privateEventsMessageOffenceAdded;

    private String request;

    private final HearingData hearingData;
    private final UpdatedOffenceData updatedOffenceData;
    private final ListedCaseData listedCaseData;
    private final DefendantData defendantData;
    private final OffenceData offenceData;
    private final UUID offenceIdToBeDeleted;
    private final UUID caseId;
    private final UUID metadataId;
    private final UUID userId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);


    public UpdateDefendantOffencesSteps(UUID caseId, HearingData hearingData, UpdatedOffenceData updatedOffenceData, UUID offenceIdToBeDeleted) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);
        this.defendantData = listedCaseData.getDefendants().get(0);
        this.offenceData = defendantData.getOffences().get(0);
        this.updatedOffenceData = updatedOffenceData;
        this.metadataId = randomUUID();
        this.userId = randomUUID();
        this.offenceIdToBeDeleted = offenceIdToBeDeleted;

        publicEventDefendantOffencesUpdated = publicEvents.createPublicProducer();
        publicEventMessageConsumerDefendantOffencesUpdated = publicEvents.createPublicConsumer(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED);

        privateEventMessageOffencesToBeUpdated = privateEvents.createPrivateConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED);
        privateEventMessageOffencesToBeDeleted = privateEvents.createPrivateConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_DELETED);
        privateEventMessageOffencesToBeAdded = privateEvents.createPrivateConsumer(PRIVATE_EVENT_OFFENCES_TO_BE_ADDED);

        privateEventsMessageOffenceUpdated = privateEvents.createPrivateConsumer(EVENT_SELECTOR_OFFENCES_UPDATED);
        privateEventsMessageOffenceAdded = privateEvents.createPrivateConsumer(EVENT_SELECTOR_OFFENCES_ADDED);
        privateEventsMessageOffenceDeleted = privateEvents.createPrivateConsumer(EVENT_SELECTOR_OFFENCES_DELETED);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantOffencesUpdatedPublicEventIsPublished() {
        OffencesForDefendantUpdated offencesForDefendantUpdated = getOffencesForDefendantUpdated(caseId, defendantData.getDefendantId());
        publishCaseDefendantOffencesUpdated(offencesForDefendantUpdated);
    }

    public void whenCaseDefendantOffencesUpdatedPublicEventIsPublishedUpdatedOnly() {
        OffencesForDefendantUpdated offencesForDefendantUpdated = getOffencesForDefendantUpdatedOnly(caseId, defendantData.getDefendantId());
        publishCaseDefendantOffencesUpdated(offencesForDefendantUpdated);
    }

    public OffencesForDefendantUpdated whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly() {
        OffencesForDefendantUpdated offencesForDefendantUpdated = getOffencesForDefendantAddedOnly(caseId, defendantData.getDefendantId());
        publishCaseDefendantOffencesUpdated(offencesForDefendantUpdated);
        return offencesForDefendantUpdated;
    }

    public void whenCaseDefendantOffencesUpdatedPublicEventIsPublishedDeletedOnly() {
        OffencesForDefendantUpdated offencesForDefendantUpdated = getOffencesForDefendantDeletedOnly(caseId, defendantData.getDefendantId());
        publishCaseDefendantOffencesUpdated(offencesForDefendantUpdated);
    }

    private void publishCaseDefendantOffencesUpdated(OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final JsonObject updateCaseDefendantDetailsObject = (JsonObject) objectToJsonValueConverter.convert(offencesForDefendantUpdated);

        sendMessage(
                publicEventDefendantOffencesUpdated,
                PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED,
                updateCaseDefendantDetailsObject,
                metadataOf(metadataId, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED).withUserId(userId.toString()).build());

        request = updateCaseDefendantDetailsObject.toString();
    }


    public void verifyPublicEventDefendantOffencesUpdatedInActiveMQ() {
        String jsonResponse = retrieveMessageString(publicEventMessageConsumerDefendantOffencesUpdated);

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
                        "          \"indictmentParticular\": \"" + updatedOffenceData.getIndictmentParticular() + "\",\n" +
                        "          \"offenceTitleWelsh\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\",\n" +
                        "          \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "          \"count\": " + offenceData.getCount() + ",\n" +
                        "          \"orderIndex\": " + offenceData.getOrderIndex() + ",\n" +
                        "          \"offenceDefinitionId\": \"" + offenceData.getOffenceDefinitionId() + "\",\n" +
                        "          \"wording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "      \"laaApplnReference\": {" +
                        "        \"applicationReference\": \"" + updatedOffenceData.getLaaReferences().get().getApplicationReference() + "\",\n" +
                        "        \"statusCode\": \"" + updatedOffenceData.getLaaReferences().get().getStatusCode() + "\",\n" +
                        "        \"statusDate\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDate().toString() + "\",\n" +
                        "        \"statusDescription\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDescription() + "\",\n" +
                        "        \"statusId\": \"" + updatedOffenceData.getLaaReferences().get().getStatusId() + "\"\n},\n" +
                        "       \"reportingRestrictions\": [\n" +
                        "       {\n" +
                        "       \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                        "       \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                        "       \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                        "       \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get() + "\"\n}\n" +
                        "      ]\n" +
                        "        }\n]\n," +
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
                        "  \"modifiedDate\": \"" + LocalDate.now() + "\",\n" +
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
                        "          \"indictmentParticular\": \"" + updatedOffenceData.getIndictmentParticular() + "\",\n" +
                        "          \"offenceTitleWelsh\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\",\n" +
                        "          \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "          \"count\": " + offenceData.getCount() + ",\n" +
                        "          \"orderIndex\": " + offenceData.getOrderIndex() + ",\n" +
                        "          \"offenceDefinitionId\": \"" + offenceData.getOffenceDefinitionId() + "\",\n" +
                        "          \"wording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "      \"laaApplnReference\": {" +
                        "        \"applicationReference\": \"" + updatedOffenceData.getLaaReferences().get().getApplicationReference() + "\",\n" +
                        "        \"statusCode\": \"" + updatedOffenceData.getLaaReferences().get().getStatusCode() + "\",\n" +
                        "        \"statusDate\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDate().toString() + "\",\n" +
                        "        \"statusDescription\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDescription() + "\",\n" +
                        "        \"statusId\": \"" + updatedOffenceData.getLaaReferences().get().getStatusId() + "\"\n},\n" +
                        "      \n" +
                        "       \"reportingRestrictions\": [\n" +
                        "       {\n" +
                        "       \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                        "       \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                        "       \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                        "       \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get() + "\"\n}\n" +
                        "      ]\n" +
                        "        }\n]\n," +
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

    public void verifyEventDefendantOffencesToBeUpdateInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventMessageOffencesToBeUpdated);
        LOGGER.debug("jsonResponse from privateEventMessageOffencesToBeUpdated: {}", jsonResponse);

        String expected =
                "{\n" +
                "  \"_metadata\": {\n" +
                "    \"context\": {\n" +
                "      \"user\": \"" + userId + "\"\n" +
                "    },\n" +
                "    \"createdAt\": \"2018-12-23T16:20:04.344Z\",\n" +
                "    \"id\": \"" + metadataId + "\",\n" +
                "    \"name\": \"" + PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED + "\",\n" +
                "    \"causation\": [\n" +
                "      \"9e02a6c2-fce9-436b-b102-b8294b4d3533\",\n" +
                "      \"542d0f32-be5b-431d-bfa0-b3a2ca8e2c63\"\n" +
                "    ],\n" +
                "    \"stream\": {\n" +
                "      \"id\": \"" + caseId + "\",\n" +
                "      \"version\": 2\n" +
                "    }\n" +
                "  },\n" +
                "  \"caseId\": \"" + caseId +"\",\n" +
                "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                "  \"hearings\": [\n" +
                "    \"" + hearingData.getId() + "\"\n" +
                "  ],\n" +
                "  \"offences\": [\n" +
                "    {\n" +
                "      \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                "      \"id\": \"" + updatedOffenceData.getOffenceId() + "\",\n" +
                "      \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() +"\",\n" +
                "      \"offenceWording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                "      \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                "      \"statementOfOffence\": {\n" +
                "         \"legislation\": \"" + updatedOffenceData.getLegislation() +"\",\n" +
                "         \"welshLegislation\": \"" + updatedOffenceData.getLegislationWelsh() +"\",\n" +
                "         \"title\": \"" + updatedOffenceData.getStatementOfOffenceTitle() +"\",\n" +
                "         \"welshTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() +"\"\n" +
                "      },\n" +
                "       \"reportingRestrictions\": [\n" +
                "       {\n" +
                "       \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                "       \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                "       \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                "       \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get().toString() + "\"\n}\n" +
                "      ],\n" +
                "      \"laaApplnReference\": {" +
                "        \"applicationReference\": \"" + updatedOffenceData.getLaaReferences().get().getApplicationReference() + "\",\n" +
                "        \"statusCode\": \"" + updatedOffenceData.getLaaReferences().get().getStatusCode() + "\",\n" +
                "        \"statusDate\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDate().toString() + "\",\n" +
                "        \"statusDescription\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDescription() + "\",\n" +
                "        \"statusId\": \"" + updatedOffenceData.getLaaReferences().get().getStatusId() + "\"\n}\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";


        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }

    public void verifyEventDefendantOffencesToBeAddedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventMessageOffencesToBeAdded);
        LOGGER.debug("jsonResponse from privateEventMessageOffencesToBeAdded: {}", jsonResponse);

        String expected =
                "{\n" +
                        "  \"_metadata\": {\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"80f5a8d8-2c64-4c38-b77c-b6582db8ae65\"\n" +
                        "    },\n" +
                        "    \"createdAt\": \"2018-12-23T17:21:34.476Z\",\n" +
                        "    \"id\": \"aaf36524-b4fa-43b0-918b-921949528f69\",\n" +
                        "    \"name\": \"listing.events.offences-to-be-added\",\n" +
                        "    \"causation\": [\n" +
                        "      \"4ea8bc4a-bc17-41fc-b998-d1a34aec4cbe\",\n" +
                        "      \"527f861d-68a2-4745-8af1-0aeea23d34cc\"\n" +
                        "    ],\n" +
                        "    \"stream\": {\n" +
                        "      \"id\": \"8aee5f13-e84d-46da-82cc-af41176a9293\",\n" +
                        "      \"version\": 4\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"caseId\": \"" + caseId + "\",\n" +
                        "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "  \"hearings\": [\n" +
                        "    \"" + hearingData.getId() + "\"\n" +
                        "  ],\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                        "      \"id\": \"" + updatedOffenceData.getRandomOffenceId() + "\",\n" +
                        "      \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() + "\",\n" +
                        "      \"offenceWording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "      \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "       \"reportingRestrictions\": [\n" +
                        "       {\n" +
                        "       \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                        "       \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                        "       \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                        "       \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get().toString() + "\"\n}\n" +
                        "      ],\n" +
                        "      \"statementOfOffence\": {\n" +
                        "         \"legislation\": \"" + updatedOffenceData.getLegislation() + "\",\n" +
                        "         \"welshLegislation\": \"" + updatedOffenceData.getLegislationWelsh() + "\",\n" +
                        "         \"title\": \"" + updatedOffenceData.getStatementOfOffenceTitle() + "\",\n" +
                        "         \"welshTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }

    public void verifyEventDefendantOffencesToBeDeletedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventMessageOffencesToBeDeleted);
        LOGGER.debug("jsonResponse from privateEventMessageOffencesToBeDeleted: {}", jsonResponse);

        String expected =
                "{\n" +
                        "  \"_metadata\": {\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"c58f3c17-b046-4fb3-938e-93029a0ad9e6\"\n" +
                        "    },\n" +
                        "    \"createdAt\": \"2018-12-23T17:27:02.016Z\",\n" +
                        "    \"id\": \"5951f96f-74a1-4faa-bd01-09b4c9d2c2b6\",\n" +
                        "    \"name\": \"listing.events.offences-to-be-deleted\",\n" +
                        "    \"causation\": [\n" +
                        "      \"9268362f-9e8d-42b2-ae05-27a67c361aad\",\n" +
                        "      \"6d2b8100-fa7c-4e11-8d23-f2f98e542df7\"\n" +
                        "    ],\n" +
                        "    \"stream\": {\n" +
                        "      \"id\": \"a7c8ed80-d4cd-4648-82ae-b3dfb99dcc2f\",\n" +
                        "      \"version\": 3\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"caseId\": \"" + caseId + "\",\n" +
                        "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "  \"hearings\": [\n" +
                        "    \"" + hearingData.getId() + "\"\n" +
                        "  ],\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "      \"id\": \"" + offenceIdToBeDeleted + "\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }

    public void verifyEventOffenceUpdatedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventsMessageOffenceUpdated);
        LOGGER.debug("jsonResponse from privateEventsMessageOffenceUpdated: {}", jsonResponse);

        String expected =
                "{\n" +
                        "  \"_metadata\": {\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"3a829dd7-9554-4529-bab5-296d46ac739f\"\n" +
                        "    },\n" +
                        "    \"createdAt\": \"2018-12-23T17:34:15.404Z\",\n" +
                        "    \"stream\": {\n" +
                        "      \"id\": \"c8ff5eb5-ebe7-4eef-978c-14587462fea0\",\n" +
                        "      \"version\": 3\n" +
                        "    },\n" +
                        "    \"id\": \"9c4a1bca-1190-4bc8-87e9-2f8085179409\",\n" +
                        "    \"name\": \"listing.events.offence-updated\",\n" +
                        "    \"causation\": [\n" +
                        "      \"22e7bd84-91c1-46f9-bc9a-352e1cc192a5\",\n" +
                        "      \"6db87020-8fc6-4f5f-a7fb-d062ef10465a\",\n" +
                        "      \"c8a8c850-64df-4373-a85c-f7d48901c437\",\n" +
                        "      \"bb882fb8-6b5c-4752-9367-9945ea7689e6\"\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"caseId\": \"" + caseId + "\",\n" +
                        "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "  \"hearingId\": \"" + hearingData.getId() + "\",\n" +
                        "  \"offence\": {\n" +
                        "      \"count\": " + updatedOffenceData.getCount() + ",\n" +
                        "      \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                        "      \"id\": \"" + updatedOffenceData.getOffenceId() + "\",\n" +
                        "      \"indictmentParticular\": \"" + updatedOffenceData.getIndictmentParticular() + "\",\n" +
                        "      \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() + "\",\n" +
                        "      \"offenceWording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "      \"orderIndex\": " + updatedOffenceData.getOrderIndex() + ",\n" +
                        "      \"restrictFromCourtList\": false,\n" +
                        "      \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "      \"statementOfOffence\": {\n" +
                        "         \"legislation\": \"" + updatedOffenceData.getLegislation() +"\",\n" +
                        "         \"welshLegislation\": \"" + updatedOffenceData.getLegislationWelsh() +"\",\n" +
                        "         \"title\": \"" + updatedOffenceData.getStatementOfOffenceTitle() +"\",\n" +
                        "         \"welshTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() +"\"\n" +
                        "      },\n" +
                        "       \"reportingRestrictions\": [\n" +
                        "       {\n" +
                        "       \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                        "       \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                        "       \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                        "       \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get().toString() + "\"\n}\n" +
                        "      ],\n" +
                        "      \"laaApplnReference\": {" +
                        "        \"applicationReference\": \"" + updatedOffenceData.getLaaReferences().get().getApplicationReference() + "\",\n" +
                        "        \"statusCode\": \"" + updatedOffenceData.getLaaReferences().get().getStatusCode() + "\",\n" +
                        "        \"statusDate\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDate().toString() + "\",\n" +
                        "        \"statusDescription\": \"" + updatedOffenceData.getLaaReferences().get().getStatusDescription() + "\",\n" +
                        "        \"statusId\": \"" + updatedOffenceData.getLaaReferences().get().getStatusId() + "\"\n}\n" +
                        "  }\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }

    public void verifyEventOffenceAddedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventsMessageOffenceAdded);
        LOGGER.debug("jsonResponse from privateEventsMessageOffenceAdded: {}", jsonResponse);

        final LaaReferenceData laaReferenceData = updatedOffenceData.getLaaReferences().get();
        String expected =
                "{\n" +
                        "  \"_metadata\": {\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"3a829dd7-9554-4529-bab5-296d46ac739f\"\n" +
                        "    },\n" +
                        "    \"createdAt\": \"2018-12-23T17:34:15.404Z\",\n" +
                        "    \"stream\": {\n" +
                        "      \"id\": \"c8ff5eb5-ebe7-4eef-978c-14587462fea0\",\n" +
                        "      \"version\": 3\n" +
                        "    },\n" +
                        "    \"id\": \"9c4a1bca-1190-4bc8-87e9-2f8085179409\",\n" +
                        "    \"name\": \"listing.events.offence-added\",\n" +
                        "    \"causation\": [\n" +
                        "      \"22e7bd84-91c1-46f9-bc9a-352e1cc192a5\",\n" +
                        "      \"6db87020-8fc6-4f5f-a7fb-d062ef10465a\",\n" +
                        "      \"c8a8c850-64df-4373-a85c-f7d48901c437\",\n" +
                        "      \"bb882fb8-6b5c-4752-9367-9945ea7689e6\"\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"caseId\": \"" + caseId + "\",\n" +
                        "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "  \"hearingId\": \"" + hearingData.getId() + "\",\n" +
                        "  \"offence\": {\n" +
                        "      \"count\": " + updatedOffenceData.getCount() + ",\n" +
                        "      \"endDate\": \"" + updatedOffenceData.getEndDate() + "\",\n" +
                        "      \"id\": \"" + updatedOffenceData.getRandomOffenceId() + "\",\n" +
                        "      \"indictmentParticular\": \"" + updatedOffenceData.getIndictmentParticular() + "\",\n" +
                        "      \"laaApplnReference\": {\n" +
                        "         \"applicationReference\": \"" + laaReferenceData.getApplicationReference() + "\",\n" +
                        "         \"statusCode\": \"" + laaReferenceData.getStatusCode() + "\",\n" +
                        "         \"statusDate\": \"" + laaReferenceData.getStatusDate() + "\",\n" +
                        "         \"statusDescription\": \"" + laaReferenceData.getStatusDescription() + "\",\n" +
                        "         \"statusId\": \"" + laaReferenceData.getStatusId() + "\"\n" +
                        "      },\n" +
                        "      \"offenceCode\": \"" + updatedOffenceData.getOffenceCode() + "\",\n" +
                        "      \"offenceWording\": \"" + updatedOffenceData.getOffenceWording() + "\",\n" +
                        "      \"orderIndex\": " + updatedOffenceData.getOrderIndex() + ",\n" +
                        "      \"reportingRestrictions\": [\n" +
                        "      {\n" +
                        "      \"id\": \"" + updatedOffenceData.getReportingRestriction().get(0).getId() + "\",\n" +
                        "      \"judicialResultId\": \"" + updatedOffenceData.getReportingRestriction().get(0).getJudicialResultId().get() + "\",\n" +
                        "      \"label\": \"" + updatedOffenceData.getReportingRestriction().get(0).getLabel() + "\",\n" +
                        "      \"orderedDate\": \"" + updatedOffenceData.getReportingRestriction().get(0).getOrderedDate().get().toString() + "\"\n}\n" +
                        "      ],\n" +
                        "      \"restrictFromCourtList\": false,\n" +
                        "      \"startDate\": \"" + updatedOffenceData.getStartDate() + "\",\n" +
                        "      \"statementOfOffence\": {\n" +
                        "         \"legislation\": \"" + updatedOffenceData.getLegislation() + "\",\n" +
                        "         \"title\": \"" + updatedOffenceData.getStatementOfOffenceTitle() + "\",\n" +
                        "         \"welshLegislation\": \"" + updatedOffenceData.getLegislationWelsh() + "\",\n" +
                        "         \"welshTitle\": \"" + updatedOffenceData.getStatementOfOffenceTitleWelsh() + "\"\n" +
                        "      }\n" +
                        "  }\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }


    public void verifyEventOffenceDeletedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        String jsonResponse = QueueUtil.retrieveMessageString(privateEventsMessageOffenceDeleted);
        LOGGER.debug("jsonResponse from privateEventsMessageOffenceDeleted: {}", jsonResponse);

        String expected =
                "{\n" +
                        "  \"_metadata\": {\n" +
                        "    \"context\": {\n" +
                        "      \"user\": \"6acc7ca4-497c-4b23-8822-60b22e213a01\"\n" +
                        "    },\n" +
                        "    \"createdAt\": \"2018-12-23T17:40:59.174Z\",\n" +
                        "    \"stream\": {\n" +
                        "      \"id\": \"bf202d9c-f2a2-4ad1-a97a-1854593a9fa3\",\n" +
                        "      \"version\": 2\n" +
                        "    },\n" +
                        "    \"id\": \"3e51039e-657f-4ccc-a750-801ccf9604b2\",\n" +
                        "    \"name\": \"listing.events.offence-deleted\",\n" +
                        "    \"causation\": [\n" +
                        "      \"4df3b3cb-a6ef-47b7-84e1-097d54fbcdba\",\n" +
                        "      \"a89dac0a-5b0a-4770-93d5-fe186b7cb751\",\n" +
                        "      \"21eaba37-2d20-4be3-8621-d0e06de18cf0\",\n" +
                        "      \"69a810e3-c161-4a47-b4e6-dedf8e76f330\"\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"defendantId\": \"" + defendantData.getDefendantId() + "\",\n" +
                        "  \"hearingId\": \"" + hearingData.getId() + "\",\n" +
                        "  \"offenceId\": \"" + offenceIdToBeDeleted + "\"\n" +
                        "}\n";

        assertEquals(expected, jsonResponse, ignoreMetaDataComparator);
    }

    public void verifyDefendentOffenceUpdatedOnlyFromAPI(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id",
                                        equalTo(updatedOffenceData.getOffenceId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].endDate",
                                        equalTo(updatedOffenceData.getEndDate().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].startDate",
                                        equalTo(updatedOffenceData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].statementOfOffence.title",
                                        equalTo(updatedOffenceData.getStatementOfOffenceTitle())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].statementOfOffence.welshTitle",
                                        equalTo(updatedOffenceData.getStatementOfOffenceTitleWelsh())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].offenceCode",
                                        equalTo(updatedOffenceData.getOffenceCode())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].offenceWording",
                                        equalTo(updatedOffenceData.getOffenceWording())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].indictmentParticular",
                                        equalTo(updatedOffenceData.getIndictmentParticular()))
                        )));
    }

    public void verifyDefendentOffenceAddedOnlyFromAPI(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].id",
                                        equalTo(updatedOffenceData.getRandomOffenceId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].endDate",
                                        equalTo(updatedOffenceData.getEndDate().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].startDate",
                                        equalTo(updatedOffenceData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].statementOfOffence.title",
                                        equalTo(updatedOffenceData.getStatementOfOffenceTitle())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].statementOfOffence.welshTitle",
                                        equalTo(updatedOffenceData.getStatementOfOffenceTitleWelsh())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].offenceCode",
                                        equalTo(updatedOffenceData.getOffenceCode())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[3].offenceWording",
                                        equalTo(updatedOffenceData.getOffenceWording()))
                        )));
    }

    public void verifyDefendentOffenceDeletedOnlyFromAPI(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences.length()",
                                        is(2))
                        )));
    }

    private OffencesForDefendantUpdated getOffencesForDefendantUpdated(UUID caseId, UUID defendantId) {

        return OffencesForDefendantUpdated.offencesForDefendantUpdated()
                .withAddedOffences(buildAddedOffences(caseId, defendantId))
                .withDeletedOffences(buildDeletedOffence(caseId, defendantId, offenceIdToBeDeleted))
                .withModifiedDate(LocalDate.now().toString())
                .withUpdatedOffences(buildUpdatedOffences(caseId, defendantId, updatedOffenceData))
                .build();
    }

    private OffencesForDefendantUpdated getOffencesForDefendantUpdatedOnly(UUID caseId, UUID defendantId) {

        return OffencesForDefendantUpdated.offencesForDefendantUpdated()
                .withModifiedDate(LocalDate.now().toString())
                .withUpdatedOffences(buildUpdatedOffences(caseId, defendantId, updatedOffenceData))
                .build();
    }

    private OffencesForDefendantUpdated getOffencesForDefendantAddedOnly(UUID caseId, UUID defendantId) {

        return OffencesForDefendantUpdated.offencesForDefendantUpdated()
                .withAddedOffences(buildAddedOffences(caseId, defendantId))
                .withModifiedDate(LocalDate.now().toString())
                .build();
    }

    private OffencesForDefendantUpdated getOffencesForDefendantDeletedOnly(UUID caseId, UUID defendantId) {

        return OffencesForDefendantUpdated.offencesForDefendantUpdated()
                .withDeletedOffences(buildDeletedOffence(caseId, defendantId, offenceIdToBeDeleted))
                .withModifiedDate(LocalDate.now().toString())
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

    private List<UpdatedOffences> buildUpdatedOffences(UUID caseId, UUID defendantId, UpdatedOffenceData updatedOffenceData) {
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
                .withEndDate(updatedOffenceData.getEndDate().toString())
                .withOffenceTitle(updatedOffenceData.getStatementOfOffenceTitle())
                .withOffenceTitleWelsh(updatedOffenceData.getStatementOfOffenceTitleWelsh())
                .withWording(updatedOffenceData.getOffenceWording())
                .withOffenceLegislation(updatedOffenceData.getLegislation())
                .withOffenceLegislationWelsh(updatedOffenceData.getLegislationWelsh())
                .withCount(offenceData.getCount())
                .withIndictmentParticular(offenceData.getIndictmentParticular())
                .withOrderIndex(offenceData.getOrderIndex())
                .withOffenceLegislation(offenceData.getOffenceLegislation())
                .withOffenceDefinitionId(offenceData.getOffenceDefinitionId())
                .withLaaApplnReference(buildLaaReference(updatedOffenceData.getLaaReferences().get()))
                .withReportingRestrictions(buildReportingRestriction(updatedOffenceData.getReportingRestriction()))
                .build();
    }

    private LaaReference buildLaaReference(LaaReferenceData laaReferenceData) {

        return laaReference()
                .withStatusCode(laaReferenceData.getStatusCode())
                .withStatusDescription(laaReferenceData.getStatusDescription())
                .withStatusId(laaReferenceData.getStatusId())
                .withStatusDate(String.valueOf((laaReferenceData.getStatusDate())))
                .withApplicationReference(laaReferenceData.getApplicationReference())
                .build();
    }

    private List<ReportingRestriction> buildReportingRestriction(final List<ReportingRestrictionData> reportingRestrictionDataList) {
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        reportingRestrictionDataList
                .forEach(reportingRestrictionData -> reportingRestrictions.add(ReportingRestriction.reportingRestriction()
                        .withId(reportingRestrictionData.getId())
                        .withLabel(reportingRestrictionData.getLabel())
                        .withJudicialResultId(reportingRestrictionData.getJudicialResultId().orElse(null))
                        .withOrderedDate(reportingRestrictionData.getOrderedDate().get().toString())
                        .build()));

        return reportingRestrictions;
    }

}

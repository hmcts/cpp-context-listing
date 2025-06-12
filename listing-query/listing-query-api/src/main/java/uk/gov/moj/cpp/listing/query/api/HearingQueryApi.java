package uk.gov.moj.cpp.listing.query.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataLoader;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_API)
public class HearingQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryApi.class);

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String COURT_ROOM_ID = "courtRoomId";
    public static final String RESTRICTED = "restricted";
    private static final String LIST_ID = "listId";
    private static final String OU_L2_CODE = "oucodeL2Code";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String DOB = "dateOfBirth";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String HEARING_DATE = "hearingDate";
    private static final String IS_CIVIL = "isCivil";
    private static final String IS_GROUP_MEMBER = "isGroupMember";

    @Inject
    private HearingQueryView hearingQueryView;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataLoader referenceDataLoader;

    @Inject
    private StandardPublicCourtListTemplateAssembler standardPublicCourtListAssembler;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;



    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        return hearingQueryView.searchHearings(query);
    }

    @Handles("listing.unscheduled.search.hearings")
    public Envelope<JsonObject> searchUnscheduledHearings(final JsonEnvelope query) {
        final JsonObject jsonObject = query.payloadAsJsonObject();
        final String courtCentreId = jsonObject.getString(COURT_CENTRE_ID, null);
        final String oucodeL2Code = jsonObject.getString(OU_L2_CODE, null);
        final JsonObjectBuilder objectBuilder = createObjectBuilderWithFilter(jsonObject,
                keyName -> (!COURT_CENTRE_ID.equalsIgnoreCase(keyName)) && (!OU_L2_CODE.equalsIgnoreCase(keyName)));

        if (Strings.isNullOrEmpty(courtCentreId)) {
            if (!Strings.isNullOrEmpty(oucodeL2Code)) {
                final List<OrganisationUnit> organisationUnits = referenceDataLoader.fetchOrganisationUnitsByOucodeL2Code(oucodeL2Code);
                final String courtCentreIds = organisationUnits.stream().map(OrganisationUnit::getId).map(UUID::toString).collect(Collectors.joining(","));
                objectBuilder.add(COURT_CENTRE_IDS, courtCentreIds);
            }
        } else {
            objectBuilder.add(COURT_CENTRE_IDS, courtCentreId);
        }
        return hearingQueryView.searchUnscheduledHearings(envelopeFrom(query.metadata(), objectBuilder.build()));
    }

    @Handles("listing.available.search.hearings")
    public JsonEnvelope searchAvailableHearings(final JsonEnvelope query) throws IOException {
        return hearingQueryView.searchAvailableHearings(query);
    }

    @Handles("listing.allocated.and.unallocated.hearings")
    public JsonEnvelope searchUnallocatedHearings(final JsonEnvelope query) {
        return hearingQueryView.searchAllocatedAndUnallocatedHearings(query);
    }
    @Handles("listing.any-allocation.search.hearings")
    public Envelope<JsonObject> searchHearingsWithAnyAllocationState(final JsonEnvelope query)  {
        return hearingQueryView.searchHearingsWithAnyAllocationState(query);
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSsearchHearings(final JsonEnvelope query) {
        return hearingQueryView.rangeSearchHearings(query);
    }

    @Handles("listing.range.search.hearings.court.calendar")
    public JsonEnvelope rangeSearchHearingsForCourtCalendar(final JsonEnvelope query) {
        return hearingQueryView.rangeSearchHearingsForCourtCalendar(query);
    }
    @Handles("listing.cotr.search.hearings")
    public JsonEnvelope searchHearingsForCotr(final JsonEnvelope query) {
        return hearingQueryView.searchHearingsForCotr(query);
    }

    @Handles("listing.search.court.list")
    public JsonEnvelope searchHearingsForCourtList(final JsonEnvelope query) {
        return query;
    }

    @Handles("listing.search.court.list.payload")
    public JsonEnvelope searchHearingsForCourtListPayload(final JsonEnvelope query) {
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String listId = query.payloadAsJsonObject().getString(LIST_ID);
        final boolean restricted = Optional.ofNullable(query.payloadAsJsonObject().get(RESTRICTED)).map(restrictedJson -> Boolean.valueOf(restrictedJson.toString())).orElse(false);
        final Optional<CourtListType> courtListType = CourtListType.valueFor(listId);

        if(courtListType.isPresent()) {
            final JsonEnvelope queryResponse = hearingQueryView.getCourtListContent(query);
            final Optional<JsonObject> courtListData = standardPublicCourtListAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType.get(), restricted);
            if (courtListData.isPresent()) {
                final JsonObject courtListPayload = courtListData.get();
                final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(queryResponse, courtCentreId).orElse(false);
                final String templateName = getTemplateName(courtListType.get(), isWelsh);
                final JsonObjectBuilder builder = Json.createObjectBuilder();
                courtListPayload.forEach(builder::add);
                builder.add("templateName", templateName);
                return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list.payload"), builder);
            }
        }
        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list.payload"), createObjectBuilder());
    }

    @Handles("listing.search.hearing")
    public JsonEnvelope searchForHearingById(final JsonEnvelope query) {
        ensureThatHearingIdIsAValidUUID(query);
        return hearingQueryView.getHearingById(query);
    }

    private void ensureThatHearingIdIsAValidUUID(final JsonEnvelope query) {
        final String rawId = query.payloadAsJsonObject().getString("id", null);
        if (rawId == null) {
            final String message = "Attempted to search for a Hearing without an ID.";
            LOGGER.warn(message);
            throw new IllegalArgumentException(message);
        }
        try {
            UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Please ensure that the id is a valid UUID.", ex);
        }
    }

    @Handles("listing.court.list.publish.status")
    public JsonEnvelope publishCourtListStatus(final JsonEnvelope query) {
        return hearingQueryView.getCourtListPublishStatus(query);
    }

    @Handles("listing.get.cases-by-person-defendant")
    public JsonEnvelope getCasesByPersonDefendantAndHearingDate(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();
        final String firstName = payload.getString(FIRST_NAME);
        final String lastName = payload.getString(LAST_NAME);
        final String dateOfBirth = payload.getString(DOB);
        final String hearingDate = payload.getString(HEARING_DATE);
        final boolean isCivilParameterExists = payload.containsKey(IS_CIVIL);
        final boolean isGroupMemberParameterExists = payload.containsKey(IS_GROUP_MEMBER);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(FIRST_NAME, firstName)
                .add(LAST_NAME, lastName)
                .add(DOB, dateOfBirth);

        if (isCivilParameterExists) {
            jsonObjectBuilder.add(IS_CIVIL, payload.getBoolean(IS_CIVIL));
        }
        if (isGroupMemberParameterExists) {
            jsonObjectBuilder.add(IS_GROUP_MEMBER, payload.getBoolean(IS_GROUP_MEMBER));
        }
        final JsonObject queryParams = jsonObjectBuilder.build();

        final Envelope<JsonObject> response = requester.request(JsonEnvelope.envelopeFrom(metadataFrom(query.metadata())
                .withName("defence.query.get-case-by-person-defendant"), queryParams), JsonObject.class);

        return processGetCaseByDefendant(response.payload(), hearingDate, query);

    }

    @Handles("listing.get.cases-by-organisation-defendant")
    public JsonEnvelope getCasesByOrganisationDefendantAndHearingDate(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();
        final String firstName = payload.getString(ORGANISATION_NAME);
        final String hearingDate = payload.getString(HEARING_DATE);
        final boolean isGroupMemberParameterExists = payload.containsKey(IS_GROUP_MEMBER);
        final boolean isCivilParameterExists = payload.containsKey(IS_CIVIL);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ORGANISATION_NAME, firstName);

        if (isCivilParameterExists) {
            jsonObjectBuilder.add(IS_CIVIL,payload.getBoolean(IS_CIVIL));
        }
        if (isGroupMemberParameterExists) {
            jsonObjectBuilder.add(IS_GROUP_MEMBER, payload.getBoolean(IS_GROUP_MEMBER));
        }
        final JsonObject queryParams = jsonObjectBuilder.build();

        final Envelope<JsonObject> response = requester.request(JsonEnvelope.envelopeFrom(metadataFrom(query.metadata())
                .withName("defence.query.get-case-by-organisation-defendant"), queryParams), JsonObject.class);

        return processGetCaseByDefendant(response.payload(), hearingDate, query);
    }

    private JsonEnvelope processGetCaseByDefendant(final JsonObject payload, final String hearingDate, final  JsonEnvelope query ){
        final List<UUID> caseIds = payload.getJsonArray("caseIds").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        final List<UUID> defendants = payload.getJsonArray("defendants").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        return  this.hearingQueryView.getCasesByDefendantAndHearingDate(caseIds, defendants, hearingDate, query);
    }


    private String getTemplateName(final CourtListType courtListType, boolean welsh) {
        if ((PUBLIC.equals(courtListType)|| ONLINE_PUBLIC.equals(courtListType)) && welsh) {
            return courtListType.getWelshTemplateName();
        }
        return courtListType.getTemplateName();
    }

}

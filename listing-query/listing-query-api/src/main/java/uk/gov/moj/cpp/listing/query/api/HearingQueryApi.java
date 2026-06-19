package uk.gov.moj.cpp.listing.query.api;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import com.google.common.base.Strings;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_API)
public class HearingQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryApi.class);

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String COURT_ROOM_ID = "courtRoomId";
    public static final String RESTRICTED = "restricted";
    public static final String INCLUDE_APPLICATIONS = "includeApplications";
    private static final String LIST_ID = "listId";
    private static final String OU_L2_CODE = "oucodeL2Code";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String DOB = "dateOfBirth";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String HEARING_DATE = "hearingDate";
    private static final String IS_CIVIL = "isCivil";
    private static final String IS_GROUP_MEMBER = "isGroupMember";
    public static final String HEARINGS = "hearings";
    public static final String COURT_APPLICATIONS = "courtApplications";
    public static final String COURT_LISTS = "courtLists";
    public static final String CREST_COURT_SITE = "crestCourtSite";
    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";

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

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        return enrichWithApplicationTypeCode(hearingQueryView.searchHearings(query));
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
    public Envelope<JsonObject> searchHearingsWithAnyAllocationState(final JsonEnvelope query) {
        return hearingQueryView.searchHearingsWithAnyAllocationState(query);
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSsearchHearings(final JsonEnvelope query) {
        return hearingQueryView.rangeSearchHearings(query);
    }

    @Handles("listing.range.search.hearings.court.calendar")
    public JsonEnvelope rangeSearchHearingsForCourtCalendar(final JsonEnvelope query) {
        return enrichWithApplicationTypeCode(hearingQueryView.rangeSearchHearingsForCourtCalendar(query));
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
        final boolean includeApplications = Optional.ofNullable(query.payloadAsJsonObject().get(INCLUDE_APPLICATIONS)).map(includeApplicationsJson -> Boolean.valueOf(includeApplicationsJson.toString())).orElse(false);
        final Optional<CourtListType> courtListType = CourtListType.valueFor(listId);

        if(courtListType.isPresent()) {
            final JsonEnvelope queryResponse = hearingQueryView.getCourtListContent(query);
            final Optional<JsonObject> courtListData = standardPublicCourtListAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType.get(), restricted, includeApplications);
            if (courtListData.isPresent()) {
                final JsonObject courtListPayload = courtListData.get();
                final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(queryResponse, courtCentreId).orElse(false);
                final String templateName = getTemplateName(courtListType.get(), isWelsh);
                final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
                courtListPayload.forEach(builder::add);
                builder.add("templateName", templateName);
                final JsonObject responsePayload = builder.build();

                return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list.payload"), responsePayload);
            }
        }
        final JsonObject emptyResponse = createObjectBuilder().build();

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list.payload"), emptyResponse);
    }

    @Handles("listing.search.daily.list.payload")
    public JsonEnvelope getDailyList(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final String courtCentreId = payload.getString(COURT_CENTRE_ID);
        final String documentType = payload.getString("publishCourtListType");

        final String weekCommencingStartDate = payload.getString(WEEK_COMMENCING_START_DATE, null);
        final boolean isWeekCommencing = weekCommencingStartDate != null;
        final String startDate = isWeekCommencing ? weekCommencingStartDate : payload.getString("startDate");
        final String weekCommencingEndDate = isWeekCommencing ? payload.getString(WEEK_COMMENCING_END_DATE, null) : null;
        final String endDate = isWeekCommencing ? weekCommencingEndDate : payload.getString("endDate", null);

        final JsonObjectBuilder requestPayloadBuilder = JsonObjects.createObjectBuilder()
                .add(COURT_CENTRE_ID, courtCentreId)
                .add("startDate", startDate)
                .add("publishCourtListType", documentType);
        if (endDate != null) {
            requestPayloadBuilder.add("endDate", endDate);
        }
        final JsonEnvelope courtListEnvelope = envelopeFrom(
                metadataFrom(query.metadata()).withName("listing.courtlist"),
                requestPayloadBuilder.build());
        final JsonEnvelope response = hearingQueryView.retrieveCourtList(courtListEnvelope);

        final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(query, courtCentreId).orElse(false);
        final JsonObject courtCentreJson = referenceDataService.getCourtCentreById(UUID.fromString(courtCentreId), query).payloadAsJsonObject();
        final String courtCentreName = courtCentreJson.getString("oucodeL3Name", null);
        final String welshCourtCentreName = courtCentreJson.getString("oucodeL3WelshName", null);
        final String address1 = courtCentreJson.getString("address1", null);
        final String address2 = courtCentreJson.getString("address2", null);

        final JsonObject responsePayload = response.payloadAsJsonObject();
        final JsonObjectBuilder enrichedBuilder = JsonObjects.createObjectBuilder();
        responsePayload.forEach((key, value) -> {
            if (COURT_LISTS.equals(key)) {
                enrichedBuilder.add(COURT_LISTS, enrichCourtListsWithAddress(responsePayload.getJsonArray(COURT_LISTS), address1, address2));
            } else {
                enrichedBuilder.add(key, value);
            }
        });
        if (isWeekCommencing) {
            enrichedBuilder.add(WEEK_COMMENCING_START_DATE, weekCommencingStartDate);
            if (weekCommencingEndDate != null) {
                enrichedBuilder.add(WEEK_COMMENCING_END_DATE, weekCommencingEndDate);
            }
        } else {
            enrichedBuilder.add("sittingDate", startDate);
        }
        enrichedBuilder.add("publishedAt", LocalDate.now().toString());
        enrichedBuilder.add("documentType", documentType);
        enrichedBuilder.add("isWelsh", isWelsh);
        if (courtCentreName != null) {
            enrichedBuilder.add("courtCentreName", courtCentreName);
        }
        if (welshCourtCentreName != null) {
            enrichedBuilder.add("welshCourtCentreName", welshCourtCentreName);
        }

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.daily.list.payload"), enrichedBuilder.build());
    }

    private JsonArray enrichCourtListsWithAddress(final JsonArray courtLists, final String address1, final String address2) {
        final JsonArrayBuilder enrichedCourtListsBuilder = createArrayBuilder();
        courtLists.getValuesAs(JsonObject.class).forEach(courtList -> {
            final JsonObject crestCourtSite = courtList.getJsonObject(CREST_COURT_SITE);
            final JsonObjectBuilder enrichedSiteBuilder = JsonObjects.createObjectBuilder();
            crestCourtSite.forEach(enrichedSiteBuilder::add);
            if (address1 != null) {
                enrichedSiteBuilder.add("courtCentreAddress1", address1);
            }
            if (address2 != null) {
                enrichedSiteBuilder.add("courtCentreAddress2", address2);
            }
            final JsonObjectBuilder enrichedCourtListBuilder = JsonObjects.createObjectBuilder();
            courtList.forEach((key, value) -> {
                if (CREST_COURT_SITE.equals(key)) {
                    enrichedCourtListBuilder.add(CREST_COURT_SITE, enrichedSiteBuilder.build());
                } else {
                    enrichedCourtListBuilder.add(key, value);
                }
            });
            enrichedCourtListsBuilder.add(enrichedCourtListBuilder.build());
        });
        return enrichedCourtListsBuilder.build();
    }

    @Handles("listing.search.hearing")
    public JsonEnvelope searchForHearingById(final JsonEnvelope query) {
        ensureThatHearingIdIsAValidUUID(query);
        return hearingQueryView.getHearingById(query);
    }

    private void ensureThatHearingIdIsAValidUUID(final JsonEnvelope query) {
        final String rawId = query.payloadAsJsonObject().getString("id", null);
        if (isNull(rawId)) {
            final String message = "Attempted to search for a Hearing without an ID.";
            LOGGER.warn(message);
            throw new IllegalArgumentException(message);
        }
        try {
            fromString(rawId);
        } catch (final IllegalArgumentException ex) {
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
        final String dateOfBirth = payload.getString(DOB, null);
        final String hearingDate = payload.getString(HEARING_DATE);
        final boolean isCivilParameterExists = payload.containsKey(IS_CIVIL);
        final boolean isGroupMemberParameterExists = payload.containsKey(IS_GROUP_MEMBER);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(FIRST_NAME, firstName)
                .add(LAST_NAME, lastName);

        if (isNotEmpty(dateOfBirth)) {
            jsonObjectBuilder.add(DOB, dateOfBirth);
        }

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
            jsonObjectBuilder.add(IS_CIVIL, payload.getBoolean(IS_CIVIL));
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

        return this.hearingQueryView.getCasesByDefendantAndHearingDate(caseIds, defendants, hearingDate, query);
    }

    private String getTemplateName(final CourtListType courtListType, boolean welsh) {
        if ((PUBLIC.equals(courtListType) || ONLINE_PUBLIC.equals(courtListType)) && welsh) {
            return courtListType.getWelshTemplateName();
        }
        return courtListType.getTemplateName();
    }

    private JsonEnvelope enrichWithApplicationTypeCode(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        if (!payload.containsKey(HEARINGS) || payload.isNull(HEARINGS) || payload.getJsonArray(HEARINGS).isEmpty()) {
            return envelope;
        }

        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();

        payload.getJsonArray(HEARINGS).stream().map(hearingValue -> {
            final JsonObject hearing = (JsonObject) hearingValue;
            if (hearing.containsKey(COURT_APPLICATIONS) && !hearing.isNull(COURT_APPLICATIONS)) {
                final JsonArray courtApplications = hearing.getJsonArray(COURT_APPLICATIONS);
                if (!courtApplications.isEmpty()) {
                    final AtomicBoolean modified = new AtomicBoolean(false);
                    final JsonArrayBuilder applicationsBuilder = buildApplicationsWithEnrichedPayload(envelope, courtApplications, modified);

                    if (modified.get()) {
                        return buildHearingPayloadWithUpdatedApplications(hearing, applicationsBuilder);
                    }
                }
            }

            return hearing;

        }).forEach(hearingsBuilder::add);

        final JsonObjectBuilder payloadBuilder = buildResponsePayloadWithUpdatedHearing(payload, hearingsBuilder);

        return envelopeFrom(envelope.metadata(), payloadBuilder.build());
    }

    private JsonArrayBuilder buildApplicationsWithEnrichedPayload(final JsonEnvelope envelope, final JsonArray courtApplications, final AtomicBoolean modified) {
        final JsonArrayBuilder applicationsBuilder = createArrayBuilder();

        courtApplications.forEach(appValue -> {
                final JsonObject application = (JsonObject) appValue;
                try {
                    final UUID applicationId = fromString(application.getString("id"));
                    final Optional<JsonObject> appDetails = progressionService.getApplicationDetails(envelope, applicationId);

                    if (appDetails.isPresent()) {
                        final CourtApplication courtApplicationObj = jsonObjectToObjectConverter.convert(appDetails.get().getJsonObject("courtApplication"), CourtApplication.class);
                        final JsonObjectBuilder appBuilder = Json.createObjectBuilder();
                        application.forEach(appBuilder::add);
                        appBuilder.add("applicationTypeCode", courtApplicationObj.getType().getCode());
                        applicationsBuilder.add(appBuilder.build());
                        modified.set(true);
                        return;
                    }
                } catch (final Exception e) {
                    LOGGER.error("Failed to fetch application type code for application: " + application, e);
                }
                applicationsBuilder.add(application);
        });
        return applicationsBuilder;
    }

    private static JsonObject buildHearingPayloadWithUpdatedApplications(final JsonObject hearing, final JsonArrayBuilder applicationsBuilder) {
        final JsonObjectBuilder hearingBuilder = Json.createObjectBuilder();
        hearing.forEach((key, value) -> {
            if (!COURT_APPLICATIONS.equals(key)) {
                hearingBuilder.add(key, value);
            }
        });
        hearingBuilder.add(COURT_APPLICATIONS, applicationsBuilder.build());
        return hearingBuilder.build();
    }

    private static @NonNull JsonObjectBuilder buildResponsePayloadWithUpdatedHearing(final JsonObject payload, final JsonArrayBuilder hearingsBuilder) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payload.forEach((key, value) -> {
            if (!HEARINGS.equals(key)) {
                payloadBuilder.add(key, value);
            }
        });
        payloadBuilder.add(HEARINGS, hearingsBuilder.build());
        return payloadBuilder;
    }

}

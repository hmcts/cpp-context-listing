package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class XhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCE_DATA_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNITS = "referencedata.query.organisationunits";
    private static final String XHIBIT_COURT_MAPPINGS_QUERY_PARAM = "ouId";
    private static final String UNMAPPED_COURT_ROOM_NAME = "Court -99";

    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public List<UUID> getCourtCentreIdsForCrestId(final Envelope envelope, final String crownCourtCrestId) {

        final List<CourtLocation> courtLocations = getSitesForCrownCourt(crownCourtCrestId);

        return courtLocations.stream()
                .map(courtLocation -> getOrganisationUnitId(envelope, courtLocation.getOuCode()))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<CourtLocation> getSitesForCrownCourt(final String crownCourtCrestId) {

        final JsonObject queryParameters = createObjectBuilder().build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("cpXhibitCourtMappings").getValuesAs(JsonObject.class)
                .stream()
                .filter(court -> court.getString("crestCourtId").equals(crownCourtCrestId))
                .map(this::createCourtLocation)
                .collect(Collectors.toList());
    }

    private UUID getOrganisationUnitId(final Envelope envelope, final String ouCode) {
        final JsonObject queryParameters = createObjectBuilder().add("oucode", ouCode).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_ORGANISATION_UNITS)
                .withMetadataFrom(envelope);

        final JsonObject organisationUnit = requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("organisationunits").getValuesAs(JsonObject.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException(format("Cannot find organisation unit with code %s", ouCode)));

        return UUID.fromString(organisationUnit.getString("id"));
    }

    public CourtLocation getCourtDetails(final UUID courtCentreId) {

        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final JsonObject court = requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("cpXhibitCourtMappings").getValuesAs(JsonObject.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException(format("Cannot find court details with courtCentre %s", courtCentreId)));

        return createCourtLocation(court);
    }

    public int getCourtRoomNumber(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomMapping courtRoomMapping = getCourtRoomMappingBy(courtCentreId, courtRoomId);
        return courtRoomNumberForCourtRoom(courtRoomMapping.getCrestCourtRoomName());
    }

    private CourtRoomMapping getCourtRoomMappingBy(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomMappingsList courtRoomMappingsList = getCourtRoomMappingsList(courtCentreId.toString());

        return courtRoomMappingsList
                .getCpXhibitCourtRoomMappings()
                .stream()
                .filter(courtRoomMappings -> Objects.nonNull(courtRoomMappings.getCourtRoomUUID()))
                .filter(courtRoomMappings -> courtRoomMappings.getCourtRoomUUID().equals(courtRoomId))
                .findFirst()
                .orElse(new CourtRoomMapping(UNMAPPED_COURT_ROOM_NAME));
    }

    private CourtRoomMappingsList getCourtRoomMappingsList(final String courtCentreId) {
        final JsonObject query = createObjectBuilder()
                .add(XHIBIT_COURT_MAPPINGS_QUERY_PARAM, courtCentreId)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                query);

        return requester
                .requestAsAdmin(jsonEnvelope, CourtRoomMappingsList.class)
                .payload();
    }

    private int courtRoomNumberForCourtRoom(final String courtRoomName) {

        final String[] splitName = courtRoomName.split(" ");

        return Integer.parseInt(splitName[splitName.length-1]);
    }

    public JsonObject getJudiciary(final JsonEnvelope envelope, final UUID judiciaryId) {

        final JsonObject queryParameters = createObjectBuilder().add("ids", judiciaryId.toString()).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES)
                .withMetadataFrom(envelope);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("judiciaries")
                .getValuesAs(JsonObject.class).get(0);
    }

    public JsonObject getXhibitHearingType(final JsonEnvelope envelope, final UUID cppHearingTypeId) {

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(createObjectBuilder().build())
                .withName(REFERENCE_DATA_HEARING_TYPES)
                .withMetadataFrom(envelope);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject()
                .getJsonArray("hearingTypes").getValuesAs(JsonObject.class).stream()
                .filter(h -> UUID.fromString(h.getString("id")).equals(cppHearingTypeId))
                .findFirst().orElseThrow(() -> new RuntimeException(format("Cannot find hearing type %s", cppHearingTypeId)));
    }

    private CourtLocation createCourtLocation(final JsonObject jsonObject) {
        final String oucode = jsonObject.getString("oucode", EMPTY);
        final String crestCourtId = jsonObject.getString("crestCourtId", EMPTY);
        final String crestCourtSiteId = jsonObject.getString("crestCourtSiteId", EMPTY);
        final String crestCourtName = jsonObject.getString("crestCourtName", EMPTY);
        final String crestCourtShortName = jsonObject.getString("crestCourtShortName", EMPTY);
        final String crestCourtSiteName = jsonObject.getString("crestCourtSiteName", EMPTY);
        final String crestCourtSiteCode = jsonObject.getString("crestCourtSiteCode", EMPTY);
        final String courtType = jsonObject.getString("courtType", EMPTY);

        xhibitReferenceDataValidator.validate("crestCourtSiteId", crestCourtSiteId, jsonObject);

        return new CourtLocation(
                oucode,
                crestCourtId,
                crestCourtSiteId,
                crestCourtName,
                crestCourtShortName,
                crestCourtSiteName,
                crestCourtSiteCode,
                courtType);
    }
}

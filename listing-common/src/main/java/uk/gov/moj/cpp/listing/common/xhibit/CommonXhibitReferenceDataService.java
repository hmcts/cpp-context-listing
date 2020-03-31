package uk.gov.moj.cpp.listing.common.xhibit;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

public abstract class CommonXhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNITS = "referencedata.query.organisationunits";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCE_DATA_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS_OBJECT_NAME = "cpXhibitCourtMappings";
    private static final String XHIBIT_COURT_MAPPINGS_QUERY_PARAM = "ouId";
    private static final String UNMAPPED_COURT_ROOM_NAME = "Court -99";
    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final String DEFAULT_CREST_COURT_SITE_CODE = "A";

    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    public abstract Requester getRequester();

    public List<UUID> getCourtCentreIdsForCrestId(final Envelope envelope, final String crownCourtCrestId) {
        final List<CourtLocation> courtLocations = getSitesForCrownCourt(crownCourtCrestId);

        return courtLocations.stream()
                .map(courtLocation -> getOrganisationUnitId(envelope, courtLocation.getOuCode()))
                .distinct()
                .collect(Collectors.toList());
    }

    public String getDefaultCrestCourtSiteCode(final UUID courtCentreId) {

        return getCrestCourtSitesForCourtCentre(courtCentreId)
                .stream()
                .map(courtSite -> courtSite.getString(CREST_COURT_SITE_CODE))
                .sorted()
                .findFirst().orElse(DEFAULT_CREST_COURT_SITE_CODE);
    }

    public CourtLocation getCourtDetails(final UUID courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        final JsonObject court = getXhibitCourtMappings(queryParameters)
                .stream().findFirst()
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find court details with courtCentre %s", courtCentreId)));

        return createCourtLocation(court);
    }

    public List<JsonObject> getCrestCourtSitesForCourtCentre(final UUID courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();
        return getXhibitCourtMappings(queryParameters);
    }

    private List<JsonObject> getXhibitCourtMappings(final JsonObject queryParameters) {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final JsonArray cpXhibitCourtMappingsAsJsonArray = getRequester().requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject()
                .getJsonArray(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS_OBJECT_NAME);

        xhibitReferenceDataValidator.validateJsonArray(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS_OBJECT_NAME, cpXhibitCourtMappingsAsJsonArray);

        return cpXhibitCourtMappingsAsJsonArray.getValuesAs(JsonObject.class);
    }

    public Optional<CourtRoomMapping> getCourtRoom(final Envelope envelope, final UUID courtCentreId, final UUID courtRoomUUID) {
        final JsonObject cpCourtRoom = getCpCourtRoom(envelope, courtCentreId, courtRoomUUID);

        final Integer courtRoomId = cpCourtRoom.getJsonNumber("courtroomId").intValue();

        final JsonObject queryParameters = createObjectBuilder().add(XHIBIT_COURT_MAPPINGS_QUERY_PARAM, courtCentreId.toString()).build();

        final Optional<CourtRoomMappingsList> courtRoomMappingsListOptional = getCourtRoomMappingsList(queryParameters);

        return courtRoomMappingsListOptional.isPresent() ? courtRoomMappingsListOptional.get().getCpXhibitCourtRoomMappings()
                .stream()
                .filter(courtRoomMapping -> courtRoomMapping.getCourtRoomId().equals(courtRoomId))
                .findFirst()
                : Optional.empty();
    }

    public JsonObject getXhibitHearingType(final JsonEnvelope envelope, final UUID cppHearingTypeId) {

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(createObjectBuilder().build())
                .withName(REFERENCE_DATA_HEARING_TYPES)
                .withMetadataFrom(envelope);

        return getRequester().requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject()
                .getJsonArray("hearingTypes").getValuesAs(JsonObject.class).stream()
                .filter(h -> UUID.fromString(h.getString("id")).equals(cppHearingTypeId))
                .findFirst().orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find hearing type %s", cppHearingTypeId)));
    }

    public int getCourtRoomNumber(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomMapping courtRoomMapping = getCourtRoomMappingBy(courtCentreId, courtRoomId);
        return courtRoomNumberForCourtRoom(courtRoomMapping.getCrestCourtRoomName());
    }

    public Optional<CourtRoomMappingsList> getCourtRoomMappingsList(final JsonObject queryParameters) {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final Envelope<CourtRoomMappingsList> response = getRequester().requestAsAdmin(requestEnvelope, CourtRoomMappingsList.class);

        return Objects.isNull(response) ? Optional.empty() : Optional.of(response.payload());
    }

    public JsonObject getJudiciary(final JsonEnvelope envelope, final UUID judiciaryId) {
        final JsonObject queryParameters = createObjectBuilder().add("ids", judiciaryId.toString()).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES)
                .withMetadataFrom(envelope);

        return getRequester().requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("judiciaries")
                .getValuesAs(JsonObject.class).get(0);
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

    private UUID getOrganisationUnitId(final Envelope envelope, final String ouCode) {
        final JsonObject queryParameters = createObjectBuilder().add("oucode", ouCode).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_ORGANISATION_UNITS)
                .withMetadataFrom(envelope);

        final JsonObject organisationUnit = getRequester().requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("organisationunits").getValuesAs(JsonObject.class)
                .stream().findFirst()
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find organisation unit with code %s", ouCode)));

        return UUID.fromString(organisationUnit.getString("id"));
    }

    private JsonObject getCpCourtRoom(final Envelope envelope, final UUID courtCentreId, final UUID courtRoomUUID) {
        final JsonObject queryParameters = createObjectBuilder().add("id", courtCentreId.toString()).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_COURTROOM)
                .withMetadataFrom(envelope);

        return getRequester().requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("courtrooms").getValuesAs(JsonObject.class)
                .stream().filter(c -> UUID.fromString(c.getString("id")).equals(courtRoomUUID))
                .findFirst()
                .orElseThrow(() -> new InvalidReferenceDataException("Cannot find court room uuid " + courtRoomUUID.toString()));
    }

    private CourtRoomMappingsList getCourtRoomMappingsList(final String courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add(XHIBIT_COURT_MAPPINGS_QUERY_PARAM, courtCentreId).build();

        return getCourtRoomMappingsList(queryParameters).orElseThrow(() -> new InvalidReferenceDataException("Invalid object courtRoomMappings"));
    }

    private List<CourtLocation> getSitesForCrownCourt(final String crownCourtCrestId) {
        final JsonObject queryParameters = createObjectBuilder().build();

        return getXhibitCourtMappings(queryParameters)
                .stream()
                .filter(court -> court.getString("crestCourtId").equals(crownCourtCrestId))
                .map(this::createCourtLocation)
                .collect(Collectors.toList());
    }

    private int courtRoomNumberForCourtRoom(final String courtRoomName) {
        final String[] splitName = courtRoomName.split(" ");

        return Integer.parseInt(splitName[splitName.length-1]);
    }

    private CourtLocation createCourtLocation(final JsonObject jsonObject) {
        final String oucode = jsonObject.getString("oucode", EMPTY);
        final String crestCourtId = jsonObject.getString("crestCourtId", EMPTY);
        final String crestCourtSiteId = jsonObject.getString("crestCourtSiteId", EMPTY);
        final String crestCourtName = jsonObject.getString("crestCourtName", EMPTY);
        final String crestCourtShortName = jsonObject.getString("crestCourtShortName", EMPTY);
        final String crestCourtSiteName = jsonObject.getString("crestCourtSiteName", EMPTY);
        final String crestCourtSiteCode = jsonObject.getString(CREST_COURT_SITE_CODE, EMPTY);
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

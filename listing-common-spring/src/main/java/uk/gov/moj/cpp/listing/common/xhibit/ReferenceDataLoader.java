package uk.gov.moj.cpp.listing.common.xhibit;

import lombok.RequiredArgsConstructor;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingTypesList;
import uk.gov.moj.cpp.listing.domain.referencedata.JudiciariesList;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnitList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@Component
@RequiredArgsConstructor
public class ReferenceDataLoader {

    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNITS = "referencedata.query.organisationunits";
    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_XHIBIT_MAGS_COURT_MAPPINGS = "referencedata.query.cp-xhibit-mags-court-mapping";
    private static final String REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";

    private static final String REFERENCE_DATA_ALL_HEARING_TYPES = "referencedata.query.all-hearing-types";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    private static final String XHIBIT_COURT_MAPPINGS_QUERY_PARAM = "ouId";
    private static final String REFERENCEDATA_OUCODE_QUERY_PARAM = "oucode";

    private final CppQueryClient cppQueryClient;

    public Optional<OrganisationUnit> getOrganisationUnitByOuCode(final String ouCode) {
        final JsonObject queryParameters = createObjectBuilder().add(REFERENCEDATA_OUCODE_QUERY_PARAM, ouCode).build();

        final OrganisationUnitList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ORGANISATION_UNITS, queryParameters, OrganisationUnitList.class).orElse(null);

        return (!Objects.isNull(response) && !Objects.isNull(response) && !Objects.isNull(response.getOrganisationunits()) && !response.getOrganisationunits().isEmpty()) ?
                of(response.getOrganisationunits().get(0)) : empty();
    }

    public List<OrganisationUnit> fetchOrganisationUnitsByOucodeL2Code(final String oucodeL2Code) {
        final JsonObject queryParameters = createObjectBuilder().add("oucodeL2Code", oucodeL2Code).build();

        final OrganisationUnitList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ORGANISATION_UNITS, queryParameters, OrganisationUnitList.class).orElse(null);

        return response.getOrganisationunits();
    }

    public Optional<OrganisationUnitList> getOrganisationUnitList() {

        final OrganisationUnitList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ORGANISATION_UNITS, createObjectBuilder().build(), OrganisationUnitList.class).orElse(null);

        if (Objects.isNull(response) || Objects.isNull(response) || CollectionUtils.isEmpty(response.getOrganisationunits())) {
            throw new InvalidReferenceDataException("Cannot find organisationunits");
        }

        return of(response);
    }

    public Optional<CourtMappingsList> getXhibitCrownCourtMappings(final UUID courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add(XHIBIT_COURT_MAPPINGS_QUERY_PARAM, courtCentreId.toString()).build();

        final CourtMappingsList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS, queryParameters, CourtMappingsList.class).orElse(null);

        return of(response); // legacy parity: the framework envelope was never null, so a 404 (null payload) threw NPE here
    }

    public Optional<CourtMappingsList> getXhibitCrownCourtMappings() {
        final CourtMappingsList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS, createObjectBuilder().build(), CourtMappingsList.class).orElse(null);

        return of(response); // legacy parity: the framework envelope was never null, so a 404 (null payload) threw NPE here
    }

    public Optional<CourtMappingsList> getXhibitMagsCourtMappings(final String oucode) {
        final JsonObject queryParameters = createObjectBuilder().add(REFERENCEDATA_OUCODE_QUERY_PARAM, oucode).build();

        final CourtMapping response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_XHIBIT_MAGS_COURT_MAPPINGS, queryParameters, CourtMapping.class).orElse(null);

        if (Objects.isNull(response) || Objects.isNull(response)) {
            return empty();
        }
        final CourtMapping payload = response;
        if (isEmptyMagsCourtMappingPayload(payload)) {
            return empty();
        }
        return of(new CourtMappingsList(asList(payload)));
    }

    /**
     * Referencedata returns HTTP 200 with {@code {}} when no mags mapping exists; that deserialises to a
     * {@link CourtMapping} with no id/oucode.
     */
    private static boolean isEmptyMagsCourtMappingPayload(final CourtMapping payload) {
        return payload.getId() == null && payload.getOucode() == null;
    }

    public Optional<CourtRoomMappingsList> getCourtRoomMappingsList(UUID courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add(XHIBIT_COURT_MAPPINGS_QUERY_PARAM, courtCentreId.toString()).build();

        final CourtRoomMappingsList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS, queryParameters, CourtRoomMappingsList.class).orElse(null);

        return of(response); // legacy parity: the framework envelope was never null, so a 404 (null payload) threw NPE here
    }

    public Optional<HearingTypesList> getAllHearingTypesList() {
        final HearingTypesList response = cppQueryClient.requestAsAdmin(REFERENCE_DATA_ALL_HEARING_TYPES, createObjectBuilder().build(), HearingTypesList.class).orElse(null);

        return of(response); // legacy parity: the framework envelope was never null, so a 404 (null payload) threw NPE here
    }

    public Optional<Judiciary> getJudiciary(UUID judiciaryId) {
        final JsonObject queryParameters = createObjectBuilder().add("ids", judiciaryId.toString()).build();

        final JudiciariesList response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_JUDICIARIES, queryParameters, JudiciariesList.class).orElse(null);

        return of(response.getJudiciaries().get(0)); // legacy parity (see above)
    }

    public List<JsonObject> getCpCourtRoom(final UUID courtCentreId) {
        final JsonObject queryParameters = createObjectBuilder().add("id", courtCentreId.toString()).build();

        final JsonArray courtRooms = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_COURTROOM, queryParameters)
                .orElseThrow(() -> new IllegalStateException("Not found: " + REFERENCEDATA_QUERY_COURTROOM))
                .getJsonArray("courtrooms");

        if (isEmpty(courtRooms)) {
            throw new InvalidReferenceDataException("Cannot find court room with courtCentreId " + courtCentreId.toString());
        }

        return courtRooms.getValuesAs(JsonObject.class);
    }
}
package uk.gov.moj.cpp.listing.command.hearing.processor.service;

import static jakarta.json.Json.createObjectBuilder;

import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The reference-data queries of the JEE event processor ({@code event.processor.service.ReferenceDataService},
 * MBD refactor P2.3.2): {@code referencedata.query.organisation-unit} as the system user
 * ({@code requester.requestAsAdmin}, D19). A failure to reach reference data, or no answer, fails
 * the processor step and so the command (the JEE relay retried the step, then dropped it).
 */
@Component("hearingProcessorReferenceDataService") // named: several ReferenceDataService classes are beans
public class ReferenceDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisation-unit";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    private final CppQueryClient cppQueryClient;

    public ReferenceDataService(final CppQueryClient cppQueryClient) {
        this.cppQueryClient = cppQueryClient;
    }

    public OrganisationUnit getOrganizationUnitById(final UUID courtCentreId, final Envelope event) {
        return getOrganizationUnitByIdWithAdmin(courtCentreId, event);
    }

    public OrganisationUnit getOrganizationUnitByIdWithAdmin(final UUID courtCentreId, final Envelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final OrganisationUnit organisationUnit = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ORGANISATION_UNIT, payload, OrganisationUnit.class)
                .orElseThrow(() -> new InvalidReferenceDataException("Cannot find organisationunit for courtCenter : " + courtCentreId));
        LOGGER.debug("'referencedata.query.organisation-unit' response with payload {}", organisationUnit);
        return organisationUnit;
    }

    /** {@code referencedata.query.courtroom} as the system user; the response payload (fails when there is none). */
    public JsonObject getCourtCentreById(final UUID courtCentreId, final Envelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);
        return cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_COURTROOM, payload)
                .orElseThrow(() -> new InvalidReferenceDataException("No referencedata.query.courtroom response for " + courtCentreId));
    }

    public String getOucodeFromEnvelope(final UUID courtCentreId, final Envelope event) {
        return getCourtCentreById(courtCentreId, event).getString("oucode");
    }
}

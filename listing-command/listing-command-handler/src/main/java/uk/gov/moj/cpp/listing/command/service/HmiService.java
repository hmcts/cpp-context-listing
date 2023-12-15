package uk.gov.moj.cpp.listing.command.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.command.factory.CourtCentreFactory;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class HmiService {

    public static final String OUCODE = "oucode";

    @Inject
    private StagingHmiService stagingHmiService;

    @Inject
    private CourtCentreFactory courtCentreFactory;

    public boolean isHmiEnabled(final Hearing hearing, final Envelope payload) {
        if (nonNull(hearing)) {
            final String ouCode = getOucode(hearing.getCourtCentreId(), payload);
            return stagingHmiService.isHmiListingEnabled(ofNullable(ouCode));
        }
        return false;
    }

    public boolean isHmiEnabled(final String ouCode) {
        return stagingHmiService.isHmiListingEnabled(ofNullable(ouCode));
    }

    private String getOucode(final UUID courtCentreId, final Envelope command) {
        final JsonObject organisationUnitJsonObject = courtCentreFactory.getOrganisationUnit(courtCentreId, command);
        return organisationUnitJsonObject.getString(OUCODE);
    }
}

package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.CourtCentreAdded;
import uk.gov.moj.cpp.listing.persistence.entity.CourtCentre;

import java.util.UUID;

public class CourtCentreConverter implements Converter<CourtCentreAdded, CourtCentre>{

    @Override
    public CourtCentre convert(final CourtCentreAdded courtCentreAdded) {
        return new CourtCentre(UUID.fromString(courtCentreAdded.getId()), courtCentreAdded.getCourtCentreName());
    }
}
